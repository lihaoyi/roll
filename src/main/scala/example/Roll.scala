package example

import scala.scalajs.js

import example.cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js.JsGlobals


case class Roll() extends Game {

  implicit val space = new cp.Space()
  space.damping = 0.95
  space.gravity = new cp.Vect(0, 400)

  val rock = Forms.makePoly(
    points = Seq((1111500, 111300), (1111500, 111301), (1111501, 111300)),
    density = 1,
    static = false,
    friction = 0.6,
    elasticity = 0.6
  )
  rock.setVel((0, 0))

  val svg = new js.DOMParser().parseFromString(
    scala.js.resource.apply("Demo.svg").string,
    "text/xml"
  )

  val static =
    svg.getElementById("Static")
       .children
       .foreach(Forms.processElement(_, static = true))

  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .foreach(Forms.processElement(_, static = false))

  val staticJoints =
    svg.getElementById("Joints")
       .children
       .map(Forms.processJoint(_))
       .flatten

  val player =
    Forms.processElement(svg.getElementById("Player"), static = false)


  def cameraPos = {
    player.getPos() + player.getVel()
  }

  def draw(ctx: js.CanvasRenderingContext2D) = {
    space.constraints.foreach{
      case c: cp.PivotJoint =>
        ctx.save()
        ctx.fillStyle = Color.Red
        ctx.strokeStyle = Color.Red
        ctx.translate(c.a.getPos().x, c.a.getPos().y)
        ctx.rotate(c.a.a)
        if (c.a == space.staticBody || c.b == space.staticBody) ctx.fillCircle(c.anchr1.x, c.anchr1.y, 3)
        else ctx.strokeCircle(c.anchr1.x, c.anchr1.y, 3)
        ctx.restore()
      case _ => ()
    }

    for(body <- space.bodies :+ space.staticBody){
      ctx.save()

      ctx.translate(
        body.getPos().x,
        body.getPos().y
      )

      ctx.rotate(body.a)

      body.shapeList.foreach{
        case shape: cp.PolyShape =>
          ctx.strokeStyle = Color.Red

          ctx.strokePath(
            shape
              .verts
              .toSeq
              .grouped(2)
              .map{case Seq(x, y) => (x, y)}
              .toSeq:_*
          )

        case shape: cp.SegmentShape =>
          ctx.strokeStyle = Color.Black
          ctx.strokePath(shape.a, shape.b)

          case shape: cp.CircleShape =>
          ctx.strokeStyle = Color.Red
          ctx.fillStyle = Color.Red
          ctx.beginPath()
          ctx.arc(0, 0, shape.r, 0, 6.28)
          val start = new cp.Vect(shape.r, 0).rotate(body.a)
          val end = new cp.Vect(-shape.r, 0).rotate(body.a)
          ctx.moveTo(start.x, start.y)
          ctx.lineTo(end.x, end.y)
          ctx.stroke()
      }

      ctx.restore()
    }
  }

  def update(keys: Set[Int], lines: Seq[(cp.Vect, cp.Vect)]) = {

    val baseT = 0.45
    val maxW = 25
    val decay = (maxW - baseT) / maxW
    if (keys(KeyCode.left)) {
      player.w = (player.w - baseT) * decay
    }
    if (keys(KeyCode.right)){
      player.w = (player.w + baseT) * decay
    }
    for ((p1, p2) <- lines){

      val shape = new cp.SegmentShape(
        space.staticBody,
        (p1.x, p1.y),
        (p2.x, p2.y),
        0
      )
      space.addShape(shape)
      shape.setFriction(0.6)
      shape.setElasticity(0.1)
    }
    space.step(1.0/60)
  }
}