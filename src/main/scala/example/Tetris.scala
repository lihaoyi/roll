package example

import scala.collection.mutable
import scala.scalajs.js

import ScalaJSExample.pimpedContext


case class Tetris(bounds: () => Point, reset: () => Unit) extends Game {
  implicit class Castable(x: js.Dynamic){
    def to[T] = x.asInstanceOf[T]
  }
  println("Tetris")
  import js.Dynamic.{newInstance => New}
  val cp = js.Dynamic.global.cp
  val spaceCls = cp.Space
  val space = js.Dynamic.newInstance(spaceCls)()
  space.gravity = cp.v(0, 500)

  val w = 50
  val h = 50
  val m = w * h * 0.001
  val rock = space.addBody(New(cp.Body)(m, cp.momentForBox(m, w, h)))
  rock.setVel(cp.v(400, 0))
  rock.setPos(cp.v(500, 300))
  rock.setAngle(1)
  val shape = space.addShape(New(cp.BoxShape)(rock, w, h))
  shape.setFriction(0.3)
  shape.setElasticity(0.3)
  val floor = space.addShape(New(cp.SegmentShape)(
    space.staticBody,
    cp.v(0, 840),
    cp.v(1600, 840),
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
          val body = space.addBody(New(cp.Body)(m, cp.momentForBox(m, w, h)))
          body.setPos(cp.v(x + w/2, y + h/2))
          body.setAngle(0)

          val shape = space.addShape(New(cp.BoxShape)(body, w, h))
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
      val x = rock.p.x.to[js.Number]
      val y = rock.p.y.to[js.Number]

      ctx.translate(x, y)

      ctx.rotate(rock.a.to[js.Number])

      val nums = shape.verts.to[js.Array[js.Number]]
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