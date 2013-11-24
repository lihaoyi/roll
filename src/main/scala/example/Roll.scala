package example

import scala.scalajs.js

import example.cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js.SVGElement

class Goal(space: cp.Space, goalElement: js.Element){
  var goal =
    Form.processElement(goalElement, static = true)(space)

  var won = false
  var text = "Goal"
  goal.shapes.foreach{s =>
    s.setElasticity(0)
    s.setFriction(0)
    s.setCollisionType(1)
  }

  val p = {
    val points  = goal.drawable.asInstanceOf[Drawable.Polygon].points.map(x => x: cp.Vect)
    points.reduce(_ + _) / points.length

  }
  space.addCollisionHandler(1, 1, null, (arb: cp.Arbiter, space: cp.Space) => {
    goal = new Form(
      goal.body,
      goal.shapes,
      goal.drawable,
      Color.Yellow
    )
    text = "Success!"
  }, null)

  def draw(ctx: js.CanvasRenderingContext2D) = {
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = "20pt arial"
    ctx.lineWidth = 3
    ctx.fillStyle = goal.fillStyle
    ctx.strokeStyle = goal.strokeStyle
    goal.drawable.draw(ctx)
    ctx.fillStyle = Color.Black.toString
    ctx.fillText(text, p.x, p.y)
  }
}
case class Roll() extends Game {

  implicit val space = new cp.Space()
  space.damping = 0.95
  space.gravity = new cp.Vect(0, 400)

  val rock = Form.makePoly(
    points = Seq((1111500, 111300), (1111500, 111301), (1111501, 111300)),
    density = 1,
    static = false,
    friction = 0.6,
    elasticity = 0.6
  )

  val svg = new js.DOMParser().parseFromString(
    scala.js.resource.apply("Demo.svg").string,
    "text/xml"
  )

  val static =
    svg.getElementById("Static")
       .children
       .map(Form.processElement(_, static = true))

  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .map(Form.processElement(_, static = false))

  val backgroundImg = js.globals.document.createElement("img").asInstanceOf[js.HTMLImageElement]

  backgroundImg.src = "data:image/svg+xml;base64," + js.globals.btoa(
    s"<svg xmlns='http://www.w3.org/2000/svg' width='2000' height='2000'>" +
    new js.XMLSerializer().serializeToString(svg.getElementById("Background")) +
    "</svg>"
  )

  val staticJoints =
    svg.getElementById("Joints")
       .children
       .map(Form.processJoint)
       .flatten

  val player =
    Form.processElement(svg.getElementById("Player"), static = false)
  player.shapes(0).setCollisionType(1)



  val goal = new Goal(space, svg.getElementById("Goal"))
  val strokes = new Strokes(space)

  def cameraPos = {
    player.body.getPos() + player.body.getVel()
  }
  def drawStatic(ctx: js.CanvasRenderingContext2D, w: Int, h: Int) = {
    strokes.drawStatic(ctx, w, h)

  }
  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.drawImage(backgroundImg, 0, 0)
    for(form <- static ++ dynamic :+ player if form != null){
      val body = form.body
      ctx.save()
      ctx.lineWidth = 3
      ctx.strokeStyle = form.strokeStyle
      ctx.fillStyle = form.fillStyle
      ctx.translate(
        body.getPos().x,
        body.getPos().y
      )

      ctx.rotate(body.a)
      form.drawable.draw(ctx)
      ctx.restore()
    }

    goal.draw(ctx)

    staticJoints.foreach{ jform  =>
        ctx.save()
        ctx.fillStyle = jform.fillStyle
        ctx.strokeStyle = jform.strokeStyle
        ctx.translate(jform.joint.a.getPos().x, jform.joint.a.getPos().y)
        ctx.rotate(jform.joint.a.a)
        ctx.fillCircle(jform.joint.anchr1.x, jform.joint.anchr1.y, 5)
        ctx.restore()
    }


    strokes.draw(ctx)
  }

  def update(keys: Set[Int], lines: Seq[(cp.Vect, cp.Vect)], touching: Boolean) = {

    val baseT = 0.45
    val maxW = 25
    val decay = (maxW - baseT) / maxW
    if (keys(KeyCode.left)) {
      player.body.w = (player.body.w - baseT) * decay
    }
    if (keys(KeyCode.right)){
      player.body.w = (player.body.w + baseT) * decay
    }
    strokes.update(lines, touching)

    space.step(1.0/60)
  }
}
