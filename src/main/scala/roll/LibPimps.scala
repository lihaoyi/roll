package roll
import acyclic.file
import org.scalajs.dom
import scala.scalajs.js

object Calc{

  def apply[T](thunk: => T) = new Calc(thunk)
}
class Calc[T](thunk: => T){
  var lastVal = thunk
  def apply() = lastVal
  def recalc() = {
    lastVal = thunk
    lastVal
  }
}
case class Ref[T](var v: T){
  def apply() = v
  def update(newT: T) = v = newT
}

class PointerEvent extends dom.MouseEvent {
  var width: js.Number = _
  var rotation: js.Number = _
  var pressure: js.Number = _
  var pointerType: js.Any = _
  var isPrimary: js.Boolean = _
  var tiltY: js.Number = _
  var height: js.Number = _
  var intermediatePoints: js.Any = _
  var currentPoint: js.Any = _
  var tiltX: js.Number = _
  var hwTimestamp: js.Number = _
  var pointerId: js.Number = _
  def initPointerEvent(typeArg: js.String, canBubbleArg: js.Boolean, cancelableArg: js.Boolean, viewArg: dom.Window, detailArg: js.Number, screenXArg: js.Number, screenYArg: js.Number, clientXArg: js.Number, clientYArg: js.Number, ctrlKeyArg: js.Boolean, altKeyArg: js.Boolean, shiftKeyArg: js.Boolean, metaKeyArg: js.Boolean, buttonArg: js.Number, relatedTargetArg: dom.EventTarget, offsetXArg: js.Number, offsetYArg: js.Number, widthArg: js.Number, heightArg: js.Number, pressure: js.Number, rotation: js.Number, tiltX: js.Number, tiltY: js.Number, pointerIdArg: js.Number, pointerType: js.Any, hwTimestampArg: js.Number, isPrimary: js.Boolean): Unit = ???
  def getCurrentPoint(element: dom.Element): Unit = ???
  def getIntermediatePoints(element: dom.Element): Unit = ???
  var MSPOINTER_TYPE_PEN: js.Number = _
  var MSPOINTER_TYPE_MOUSE: js.Number = _
  var MSPOINTER_TYPE_TOUCH: js.Number = _
}

object PointerEvent extends js.Object {
  /* ??? ConstructorMember(FunSignature(List(),List(),Some(TypeRef(TypeName(MSPointerEvent),List())))) */
  var MSPOINTER_TYPE_PEN: js.Number = _
  var MSPOINTER_TYPE_MOUSE: js.Number = _
  var MSPOINTER_TYPE_TOUCH: js.Number = _
}