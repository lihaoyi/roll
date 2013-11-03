package example

import scala.scalajs.js

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
object EasySeq{
  implicit class PimpedNodeList(nodes: js.NodeList) extends EasySeq[js.Node](nodes.length, nodes.apply)
  implicit class PimpedHtmlCollection(coll: js.HTMLCollection) extends EasySeq[js.Element](coll.length, coll.apply)
  implicit class Castable(x: js.Dynamic){
    def to[T] = x.asInstanceOf[T]
  }
}
