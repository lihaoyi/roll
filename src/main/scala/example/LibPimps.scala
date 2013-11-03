package example

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
