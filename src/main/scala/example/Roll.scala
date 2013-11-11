package example

import scala.scalajs.js

import ScalaJSExample.pimpedContext

import example.cp.Implicits._
import EasySeq._


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
       .foreach(Forms.processElement(_, density = 1, friction = 0.3, elasticity = 0.3, static = true))

  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .foreach(Forms.processElement(_, density = 1, friction = 0.3, elasticity = 0.3, static = false))


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
              .map{case Seq(x, y) => new cp.Vect(x, y)}
              .toSeq:_*
          )

        case shape: cp.SegmentShape =>
          ctx.strokeStyle = Color.Black
          ctx.strokePath(shape.a, shape.b)

        case shape: cp.CircleShape =>
          ctx.strokeStyle = Color.Red
          ctx.fillStyle = Color.Red
          ctx.fillCircle(
            0, 0,
            shape.radius
          )
      }

      ctx.restore()
    }
  }

  def update(keys: Set[Int]) = {
    space.step(1.0/60)
  }
}