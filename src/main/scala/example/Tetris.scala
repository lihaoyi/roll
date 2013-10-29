package example

import scala.collection.mutable
import scala.js._

import ScalaJSExample.pimpedContext


case class Tetris(bounds: () => Point, reset: () => Unit) extends Game {
  println("Tetris")
  var x = 0

  val svg = new DOMParser().parseFromString(
    scala.js.Resource("Blocks.svg").string,
    "text/xml"
  ).getElementById("Layer_1")


  def draw(ctx: CanvasRenderingContext2D) = {

    x = (x + 1) % 3
    ctx.fillStyle = Color.Red
    val children = svg.children
    for(elem <- children){
      elem.nodeName.toString match{
        case "rect" =>
          val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map(c => elem.getAttribute(c).toString.toInt)
          ctx.fillRect(x, y, w, h)
        case _ =>
      }
    }
  }
  def update(keys: Set[Int]) = {}

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
  implicit class PimpedNodeList(nodes: NodeList) extends EasySeq[Node](nodes.length, nodes.apply)
  implicit class PimpedHtmlCollection(coll: HTMLCollection) extends EasySeq[Element](coll.length, coll.apply)
}