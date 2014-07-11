package roll
package gameplay
import acyclic.file
import async.Async._
import scala.scalajs.js

import roll.cp.Implicits._
import org.scalajs.dom.extensions._
import roll.cp
import org.scalajs.dom
import scala.concurrent.{ExecutionContext, Promise, Future}
import roll.gameplay.modules._


object Level {

  case class Input(keys: Set[Int],
                   keyPresses: Set[Int],
                   touches: Seq[Touch],
                   screenSize: cp.Vect,
                   painter: dom.CanvasRenderingContext2D)



  trait Result
  object Result{
    case object Next extends Result
    case object Reset extends Result
    case object Exit extends Result
  }

  def run(src: String, inputs: Channel[Input])(implicit ec: ExecutionContext): Future[Result] = {
    val resultPromise = Promise[Result]()
    task*async{
      val initialDims = await(inputs()).screenSize


      implicit val space = new cp.Space()
      space.damping = 0.95
      space.gravity = (0, 400)

      val svgDoc = new dom.DOMParser().parseFromString(
        scala.js.bundle.apply(src).string,
        "text/xml"
      )
      println("Static!")
      val staticShapes =
        svgDoc.getElementById("Static")
              .children
              .flatMap(Form.processElement(_, static = true))

      println("Dynamic!")
      val dynamicShapes =
        svgDoc.getElementById("Dynamic")
              .children
              .flatMap(Form.processElement(_, static = false))

      val svg = svgDoc.getElementsByTagName("svg")(0).cast[dom.SVGSVGElement]

      val widest = new cp.Vect(svg.width, svg.height)

      val backgroundImg = {
        dom.extensions.Image.createBase64Svg(
          dom.btoa(
            s"<svg xmlns='http://www.w3.org/2000/svg' width='${widest.x}' height='${widest.y}'>" +
              new dom.XMLSerializer().serializeToString(svgDoc.getElementById("Background")) +
              "</svg>"
          )
        )
      }

      val clouds = new Clouds(widest)

      val staticJoints =
        svgDoc.getElementById("Joints")
          .children
          .flatMap(Form.processJoint)

      val player = new Player(Form.processElement(svgDoc.getElementById("Player"), static = false)(space)(0))

      val goal = new Goal(
        Form.processElement(svgDoc.getElementById("Goal"), static = true)(space)(0),
        () => resultPromise.success(Result.Next)
      )
      space.addCollisionHandler(1, 1, null, (arb: cp.Arbiter, space: cp.Space) => goal.hit(), null)

      val strokes = new Strokes(space)

      val lasers = new Lasers(
        player = player.form,
        laserElement = svgDoc.getElementById("Lasers"),
        query = space.segmentQueryFirst(_, _, _, 0),
        pointQuery = space.pointQueryFirst(_, _, 0),
        kill = if (player.dead == 0.0) player.dead = 1.0
      )

      var camera: Camera = new Camera.Pan(
        initialDims,
        widest,
        checkpoints = List(
          (goal.p, 1),
          (widest / 2, math.min(initialDims.x / widest.x, initialDims.y / widest.y))
        ),
        finalCamera = new Camera.Follow(
          initialDims,
          if (goal.won) goal.p
          else player.form.body.getPos() + player.form.body.getVel(),
          widest,
          1
        )
      )


      def draw(viewPort: cp.Vect, ctx: dom.CanvasRenderingContext2D) = {
        ctx.fillStyle = "#82CAFF"
        ctx.fillRect(0, 0, viewPort.x, viewPort.y)
        strokes.drawStatic(ctx, viewPort.x, viewPort.y)
        camera.transform(ctx, viewPort){ ctx =>
          ctx.lineCap = "round"
          ctx.lineJoin = "round"
          clouds.draw(ctx)
          ctx.drawImage(backgroundImg, 0, 0)

          for(form <- staticShapes ++ dynamicShapes if form != null){
            Util.draw(ctx, form)
          }

          lasers.draw(ctx)
          player.draw(ctx)
          goal.draw(ctx)

          staticJoints.foreach{ jform  =>
            ctx.save()
            ctx.fillStyle = jform.fillStyle.toString
            ctx.strokeStyle = jform.strokeStyle.toString
            ctx.translate(jform.joint.a.getPos().x, jform.joint.a.getPos().y)
            ctx.rotate(jform.joint.a.a)
            ctx.fillCircle(jform.joint.anchr1.x, jform.joint.anchr1.y, 5)
            ctx.restore()
          }

          strokes.draw(ctx)
        }
        strokes.drawStatic(ctx, viewPort.x, viewPort.y)
        goal.drawFade(ctx)
      }

      while(!resultPromise.isCompleted){

        val input = await(inputs())

        if (input.keys(KeyCode.escape)) resultPromise.success(Result.Exit)
        camera.update(input.keys, input.screenSize)
        clouds.update()

        player.update(input.keys)
        def screenToWorld(p: cp.Vect) = ((p - input.screenSize/2) / camera.scale) + camera.pos

        strokes.update(input.touches.map{
          case Touch.Down(x) =>  Touch.Down(screenToWorld(x))
          case Touch.Move(x) =>  Touch.Move(screenToWorld(x))
          case Touch.Up(x) =>  Touch.Up(screenToWorld(x))
        })

        goal.update()
        space.step(1.0/60)
        lasers.update()
        draw(input.screenSize, input.painter)
      }
    }

    resultPromise.future
  }
}
