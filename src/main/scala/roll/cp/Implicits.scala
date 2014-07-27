package roll.cp
import acyclic.file
import scala.scalajs.js
import roll.cp
import scala.scalajs.js.annotation.JSName

object Implicits {
  implicit def TupleToVect[A <% js.Number, B <% js.Number](t: (A, B)) = new cp.Vect(t._1, t._2)
  implicit def VectToTuple(t: Vect) = (t.x, t.y)
  implicit class Point(val p: cp.Vect) extends AnyVal{
    import p._
    def +(other: cp.Vect) = new cp.Vect(x + other.x, y + other.y)
    def -(other: cp.Vect) = new cp.Vect(x - other.x, y - other.y)
    def %(other: cp.Vect) = new cp.Vect(x % other.x, y % other.y)
    def <(other: cp.Vect) = x < other.x && y < other.y
    def >(other: cp.Vect) = x > other.x && y > other.y
    def /(value: Double) = new cp.Vect(x / value, y / value)
    def /(other: cp.Vect) = new cp.Vect(x / other.x, y / other.y)
    def *(value: Double) = new cp.Vect(x * value, y * value)
    def *(other: cp.Vect) = new cp.Vect(x * other.x, y * other.y)
    def dot(other: cp.Vect) = x * other.x + y * other.y
    def length: Double = Math.sqrt(lengthSquared)
    def lengthSquared = x * x + y * y
    def within(a: cp.Vect, b: cp.Vect, extra: cp.Vect = null) = {

      import math.{min, max}
      if (extra != null)
        x >= min(a.x, b.x) - extra.x &&
        x < max(a.x, b.x) + extra.y &&
        y >= min(a.y, b.y) - extra.x &&
        y < max(a.y, b.y) + extra.y
      else
        x >= min(a.x, b.x) &&
        x < max(a.x, b.x) &&
        y >= min(a.y, b.y) &&
        y < max(a.y, b.y)
    }
    def rotate(theta: Double) = {
      val (cos, sin) = (Math.cos(theta), math.sin(theta))
      new cp.Vect(cos * x - sin * y, sin * x + cos * y)
    }
  }
}
