package example.roll

import scala.scalajs.js

import example.cp.Implicits._
import org.scalajs.dom.extensions._

import example.{cp, Game}
import org.scalajs.dom


object Roll{
  def draw(ctx: dom.CanvasRenderingContext2D, form: Form) = {
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

case class Roll(viewPort: () => cp.Vect) extends Game {

  implicit val space = new cp.Space()
  space.damping = 0.95
  space.gravity = (0, 400)

  val rock = Form.makePoly(
    points = Seq((1111500, 111300), (1111500, 111301), (1111501, 111300)),
    density = 1,
    static = false,
    friction = 0.6,
    elasticity = 0.6
  )

  val svgDoc = new dom.DOMParser().parseFromString(
    scala.js.bundle.apply("BarrelWalk.svg").string,
    "text/xml"
  )

  val static =
    svgDoc.getElementById("Static")
          .children
          .flatMap(Form.processElement(_, static = true))

  val dynamic =
    svgDoc.getElementById("Dynamic")
          .children
          .flatMap(Form.processElement(_, static = false))

  val svg = svgDoc.getElementsByTagName("svg")(0).cast[dom.SVGSVGElement]

  def widest = new cp.Vect(
    svg.width,
    svg.height
  )

  val backgroundImg = {
    dom.extensions.Image.createBase64Svg(
      dom.btoa(
        s"<svg xmlns='http://www.w3.org/2000/svg' width='${widest.x}' height='${widest.y}'>" +
          new dom.XMLSerializer().serializeToString(svgDoc.getElementById("Background")) +
          "</svg>"
      )
    )
  }

  val clouds = new Clouds(widest, viewPort)

  val staticJoints =
    svgDoc.getElementById("Joints")
          .children
          .flatMap(Form.processJoint)

  val player = new Player(space, svgDoc.getElementById("Player"))



  val goal = new Goal(space, svgDoc.getElementById("Goal"))
  val strokes = new Strokes(space)

  val lasers = new Lasers(
    space,
    player.form,
    svgDoc.getElementById("Lasers"),
    () => player.dead != 0.0,
    () => player.dead = 1.0
  )

  def cameraPos =
    if (goal.won) goal.p
    else player.form.body.getPos() + player.form.body.getVel()

  def startCameraPos = goal.p


  def drawStatic(ctx: dom.CanvasRenderingContext2D, w: Int, h: Int) = {
    strokes.drawStatic(ctx, w, h)
  }

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    ctx.lineCap = "round"
    ctx.lineJoin = "round"
    clouds.draw(ctx)
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
    clouds.update()
    lasers.update()

    player.update(keys)
    strokes.update(lines, touching)

    space.step(1.0/60)
  }
}
