package example

import java.lang.Math
import scala.collection.mutable.Buffer

case class Point(x: Double, y: Double){
  def +(other: Point) = Point(x + other.x, y + other.y)
  def -(other: Point) = Point(x - other.x, y - other.y)
  def %(other: Point) = Point(x % other.x, y % other.y)
  def <(other: Point) = x < other.x && y < other.y
  def >(other: Point) = x > other.x && y > other.y
  def /(value: Double) = Point(x / value, y / value)
  def *(value: Double) = Point(x * value, y * value)
  def *(other: Point) = x * other.x + y * other.y
  def length: Double = Math.sqrt(lengthSquared)
  def lengthSquared = x * x + y * y
  def within(a: Point, b: Point, extra: Point = Point(0, 0)) = {
    import math.{min, max}
    x >= min(a.x, b.x) - extra.x &&
      x < max(a.x, b.x) + extra.y &&
      y >= min(a.y, b.y) - extra.x &&
      y < max(a.y, b.y) + extra.y
  }
  def rotate(theta: Double) = {
    val (cos, sin) = (Math.cos(theta), math.sin(theta))
    Point(cos * x - sin * y, sin * x + cos * y)
  }
}

object Point{
  implicit def TupleToPoint[A <% Double, B <% Double](t: (A, B)) = Point(t._1, t._2)
}

case class Line(p1: Point, p2: Point){
  def intersects(o: Line) = {
    val center = (p1 + p2 + o.p1 + o.p2) / 4
    val Seq(a, b, c, d) = Seq(p1, o.p1, p2, o.p2).map(_ - center)
    val ls = Seq(a->b, b->c, c->d, d->a)
    val crosses = ls.map{case (l1, l2) =>
      val l = l2 - l1
      l.x * l1.y + l.y - l1.x
    }
    val directions =
      crosses.map(x => x == math.abs(x))
        .distinct
        .length
    directions == 1
  }
  def perpDist(p: Point): Double = {
    val d = p2 - p1
    val parHit = p1 + d * (p * d) / d.lengthSquared
    val parDist = (parHit - p1).length
    if (parDist > 0 && parDist < d.length) (parHit - p).length
    else math.min((p1-p).length, (p2-p).length)
  }
}

case class Rect(p1: Point, p2: Point){
  def contains(p: Point) = p > p1 && p < p2
}

case class Circle(center: Point, radius: Double)

case class Polygon(points: Seq[Point])
