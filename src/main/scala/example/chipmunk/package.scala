package example
import scala.scalajs.js
package object chipmunk {
  type Num = js.Number
  implicit def TupleToVect[A <% js.Number, B <% js.Number](t: (A, B)) = new Vect(t._1, t._2)

}
