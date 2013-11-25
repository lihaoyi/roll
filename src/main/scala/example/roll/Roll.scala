package example.roll

import scala.scalajs.js

import example.cp.Implicits._
import scala.scalajs.extensions._
import example.roll.{Lasers, Goal, Form}
import example.{cp, Game}


object Roll{
  def draw(ctx: js.CanvasRenderingContext2D, form: Form) = {
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

  val player = new Player(space, svg.getElementById("Player"))

  val lasers = new Lasers(
    space,
    player.form,
    svg.getElementById("Lasers"),
    () => player.dead != 0.0,
    () => player.dead = 1.0
  )

  val goal = new Goal(space, svg.getElementById("Goal"))
  val strokes = new Strokes(space)

  def cameraPos = player.form.body.getPos() + player.form.body.getVel()
  def startCameraPos = goal.p
  def widest = (
    svg.childNodes(2).asInstanceOf[js.SVGSVGElement].width.baseVal.value,
    svg.childNodes(2).asInstanceOf[js.SVGSVGElement].height.baseVal.value
  )

  def drawStatic(ctx: js.CanvasRenderingContext2D, w: Int, h: Int) = {
    strokes.drawStatic(ctx, w, h)
  }

  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.drawImage(backgroundImg, 0, 0)


    for(form <- static ++ dynamic if form != null){
      Roll.draw(ctx, form)
    }

    lasers.draw(ctx)
    player.draw(ctx)
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
    lasers.update()

    player.update(keys)
    strokes.update(lines, touching)

    space.step(1.0/60)
  }
}
