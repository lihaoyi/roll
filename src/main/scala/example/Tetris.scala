package example

import scala.collection.mutable
import scala.scalajs.js

import ScalaJSExample.pimpedContext
import example.chipmunk._

import EasySeq._

class RectShape(pos: Point,
                dims: Point,
                density: Double,
                vel: Point = (0, 0),
                angularVel: Double = 0,
                friction: Double = 0.3,
                elasticity: Double = 0.3)
               (implicit space: Space){
  val mass = dims.x * dims.y * density
  val body = space.addBody(
    new Body(mass, cp.momentForBox(mass, dims.x, dims.y))
  )
  body.setVel(new Vect(vel.x, vel.y))
  body.setPos(new Vect(pos.x, pos.y))
  body.setAngVel(angularVel)
  val shape = space.addShape(BoxShape(body, dims.x, dims.y))
  shape.setFriction(friction)
  shape.setElasticity(friction)
}
case class Tetris(bounds: () => Point, reset: () => Unit) extends Game {

  println("Tetris")

  implicit val space = new Space()

  space.gravity = new Vect(0, 500)

  val rock = new RectShape(
    pos = (500, 300),
    dims = (50, 50),
    density = 0.01,
    vel = (400, 0)
  )
  val floor = space.addShape(new SegmentShape(
    space.staticBody,
    new Vect(0, 840),
    new Vect(1600, 840),
    0
  ))
  floor.setFriction(0.3)
  floor.setElasticity(0.3)
  val svg = new js.DOMParser().parseFromString(
    js.Resource("Blocks.svg").string,
    "text/xml"
  ).getElementById("Layer_1")

  val boxes = {
    val children = svg.children
    for(elem <- children) yield {
      elem.nodeName.toString match{
        case "rect" =>
          val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map(c =>
            elem.getAttribute(c).toString.toInt
          )
          Some(new RectShape(
            pos = (x + w/2, y + h/2),
            dims = (w, h),
            density = 0.01
          ))
        case _ => None
      }
    }
  }.flatten :+ rock

  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.fillStyle = Color.Red
    for(rect <- boxes){
      ctx.save()

      ctx.translate(
        rect.body.getPos().x,
        rect.body.getPos().y
      )

      ctx.rotate(rect.body.a)

      val nums = rect.shape.asInstanceOf[PolyShape].verts
      ctx.strokePath(
        nums.toSeq
            .grouped(2)
            .map{case Seq(x, y) => Point(x, y)}
            .toSeq:_*
      )

      ctx.restore()
    }
  }

  def update(keys: Set[Int]) = {
    space.step(1.0/60)
  }
}