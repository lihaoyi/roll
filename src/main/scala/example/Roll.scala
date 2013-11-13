package example

import scala.scalajs.js

import example.cp.Implicits._
import scala.scalajs.extensions._


case class Roll() extends Game {

  implicit val space = new cp.Space()
  
  space.gravity = new cp.Vect(0, 500)

  val rock = Forms.makeRect(
    pos = (500, 300),
    dims = (50, 50),
    density = 1,
    static = false
  )
  rock.setVel((400, 0))

  val svg = new js.DOMParser().parseFromString(
    js.Resource("Blocks.svg").string,
    "text/xml"
  )

  val static =
    svg.getElementById("Static")
       .children
       .foreach(Forms.processElement(_, density = 1, friction = 0.6, elasticity = 0.6, static = true))

  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .foreach(Forms.processElement(_, density = 1, friction = 0.6, elasticity = 0.6, static = false))

  val player =
    space
      .bodies
      .toSeq
      .filter(_.shapeList.toSeq.exists(_.isInstanceOf[cp.CircleShape]))
      .head

  def cameraPos = player.getPos()

  def draw(ctx: js.CanvasRenderingContext2D) = {
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

  def update(keys: Set[Int]) = {

    if (keys(KeyCode.left)) {
      js.Dynamic.global.console.log(player)
      player.w -= 0.15
    }
    if (keys(KeyCode.right)){
      js.Dynamic.global.console.log(player)
      js.Dynamic.global.player = player
      player.w += 0.15
    }

    for(body <- space.bodies :+ space.staticBody){

    }
    space.step(1.0/60)
  }
}