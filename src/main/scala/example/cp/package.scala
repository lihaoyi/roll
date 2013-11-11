package example
import scala.scalajs.js

import scala.scalajs.js.annotation.JSName

package object cp{
  type Num = js.Number
  @JSName("cp")
  object Cp extends js.Object{

    def momentForBox(m: Num, width: Num, height: Num): Num = ???
    def areaForPoly(verts: js.Array[Num]): Num = ???
    def momentForPoly(m: Num, verts: js.Array[Num], offset: Vect): Num = ???
    def centroidForPoly(verts: js.Array[Num]): Vect = ???
    def recenterPoly(verts: js.Array[Num]): Unit = ???
  }
}