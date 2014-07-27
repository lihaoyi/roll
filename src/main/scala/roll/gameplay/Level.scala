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

}

class Level(src: String, initialDims: cp.Vect) extends Level.Result{

  implicit val space = new cp.Space()
  space.damping = 0.95
  space.gravity = (0, 400)

  val svgDoc = new dom.DOMParser().parseFromString(
    scala.js.bundle.apply(src).string,
    "text/xml"
  )

  val svgElement = svgDoc.getElementsByTagName("svg")(0).cast[dom.SVGSVGElement]
  val xmlTree = Xml.parse(svgElement)(0)

  println("Static!")
  val staticShapes =
    xmlTree.get("Static").toSeq
      .flatMap(_.children)
      .flatMap(Form.processElement(_, static = true))


  println("Dynamic!")
  val dynamicShapes =
    xmlTree.get("Dynamic").toSeq
      .flatMap(_.children)
      .flatMap(Form.processElement(_, static = false))

  val widest = new cp.Vect(svgElement.width, svgElement.height)

  val backgroundImg = {
    dom.extensions.Image.createBase64Svg(
      dom.btoa(
        s"<svg xmlns='http://www.w3.org/2000/svg' width='${widest.x}' height='${widest.y}'>" +
          new dom.XMLSerializer().serializeToString(svgDoc.getElementById("Background")) +
          "</svg>"
      )
    )
  }
  println("Clouds")
  val clouds = new Clouds(widest)

  println("staticJoints")
  val staticJoints =
    xmlTree.get("Joints").toSeq
      .flatMap(_.children)
      .collect{case x: Xml.Circle => x}
      .flatMap(Form.processJoint)

  println("player")
  val player = new Player(
    Form.processElement(
      xmlTree("Special")("Player"),
      static = false
    ).head,
    widest = widest
  )

  println("Goal")
  val goal = new Goal(
    Form.processElement(xmlTree("Special")("Goal"), static = true).head
  )
  space.addCollisionHandler(1, 1, null, (arb: cp.Arbiter, space: cp.Space) => goal.hit(), null)

  val strokes = new Strokes(space)

  val lasers = new Lasers(
    player = player.form,
    laserElements = xmlTree.get("Lasers").toSeq
                           .flatMap(_.children)
                           .collect{case el: Xml.Line if el.misc.stroke == "#FF0000" => el},
    query = space.segmentQueryFirst(_, _, _, 0),
    pointQuery = space.pointQueryFirst(_, _, 0),
    kill = if (player.dead == 0.0) player.dead = 1.0
  )

  val fields: Seq[Field] = for{
    fieldElem <- xmlTree.get("Fields").toSeq
    beamElements = fieldElem.children
    (directions, fields) = beamElements.partition(_.isInstanceOf[Xml.Line])
    elem <- fields
  } yield {

    val (center, drawable, shape) = elem match{
      case Xml.Polygon(pts, misc) => (
        (0.0, 0.0),
        Drawable.Polygon(pts),
        new cp.PolyShape(space.staticBody, Form.flatten2(pts), (0, 0))
      )
      case Xml.Circle(x, y, r, misc) => (
        (x, y),
        Drawable.Circle(r),
        new cp.CircleShape(space.staticBody, r, (x, y))
      )
    }
    space.addShape(shape)
    shape.layers = Layers.Fields
    val vects: Seq[cp.Vect] = for{
      dir0 <- directions
      dir = dir0.cast[Xml.Line]
      start = new cp.Vect(dir.x1, dir.y1)
      end = new cp.Vect(dir.x2, dir.y2)
      middle = (start + end) / 2
      res = shape.pointQuery(middle)
      if res.isDefined

    } yield {
      val d = end - start
      d / d.length
    }
    println("VECTS LENGTH " + vects.length)
    Field(center, drawable, shape, vects.reduce(_ + _) / vects.length)
  }
  println("antigravity " + fields.map(_.direction).map(p => (p.x, p.y)))
  val antigravity = new Antigravity(
    fields,
    query = (s, f) => space.shapeQuery(s, f),
    pointQuery = space.pointQueryFirst(_, _, 0)
  )
  println("camera")
  var camera: Camera = new Camera.Pan(
    initialDims,
    widest,
    checkpoints = List(
      (goal.p, 1.0),
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
      antigravity.draw(ctx)
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

  def update(input: Level.Input): Level.Result = {

    if (input.keys(KeyCode.escape)) Level.Result.Exit
    else {
      camera.update(input.keys, input.screenSize)
      clouds.update()

      player.update(input.keys)
      def screenToWorld(p: cp.Vect) = ((p - input.screenSize / 2) / camera.scale) + camera.pos

      strokes.update(input.touches.map {
        case Touch.Down(x) => Touch.Down(screenToWorld(x))
        case Touch.Move(x) => Touch.Move(screenToWorld(x))
        case Touch.Up(x) => Touch.Up(screenToWorld(x))
      })

      if (goal.update()) Level.Result.Next
      else {
        antigravity.update()
        space.step(1.0 / 60)
        lasers.update()

        draw(input.screenSize, input.painter)
        this
      }
    }
  }

}
