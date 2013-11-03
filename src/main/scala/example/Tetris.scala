package example

import scala.collection.mutable
import scala.scalajs.js

import ScalaJSExample.pimpedContext
import example.chipmunk._


case class Tetris(bounds: () => Point, reset: () => Unit) extends Game {
  implicit class Castable(x: js.Dynamic){
    def to[T] = x.asInstanceOf[T]
  }
  println("Tetris")

  val space = new Space()

  space.gravity = new Vect(0, 500)

  val w = 50
  val h = 50
  val m = w * h * 0.001
  val rock = space.addBody(new Body(m, cp.momentForBox(m, w, h)))
  rock.setVel(new Vect(400, 0))
  rock.setPos(new Vect(500, 300))
  rock.setAngle(1)
  val shape = space.addShape(BoxShape(rock, w, h))
  shape.setFriction(0.3)
  shape.setElasticity(0.3)
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
    js.Dynamic.global.console.log("BOXES")
    val children = svg.children
    for(elem <- children) yield {
      elem.nodeName.toString match{
        case "rect" =>
          val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map(c =>
            elem.getAttribute(c).toString.toInt
          )
          val m = w * h * 0.001
          val body = space.addBody(new Body(m, cp.momentForBox(m, w, h)))
          body.setPos(new Vect(x + w/2, y + h/2))
          body.setAngle(0)

          val shape = space.addShape(BoxShape(body, w, h))
          shape.setFriction(0.3)
          shape.setElasticity(0.3)
          Some(body -> shape)
        case _ => None
      }
    }
  }.flatten :+ (rock -> shape)

  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.fillStyle = Color.Red
    for((rock, shape) <- boxes){
      ctx.save()

      ctx.translate(
        rock.getPos().x,
        rock.getPos().y
      )

      ctx.rotate(rock.a)

      val nums = shape.asInstanceOf[PolyShape].verts
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

  class EasySeq[T](jsLength: js.Number, jsApply: js.Number => T) extends Seq[T]{
    def length = jsLength.toInt
    def apply(x: Int) = jsApply(x)
    def iterator = new Iterator[T]{
      var index = 0
      def hasNext: scala.Boolean = index < jsLength
      def next() = {
        val res = jsApply(index)
        index += 1
        res
      }

    }
  }
  implicit class PimpedNodeList(nodes: js.NodeList) extends EasySeq[js.Node](nodes.length, nodes.apply)
  implicit class PimpedHtmlCollection(coll: js.HTMLCollection) extends EasySeq[js.Element](coll.length, coll.apply)
}