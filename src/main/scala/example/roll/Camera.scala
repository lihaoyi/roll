package example.roll

import org.scalajs.dom
import org.scalajs.dom.extensions._
import example.cp.Implicits._
import example.cp

trait Camera{
  def update(dt: Double, keys: Set[Int])
  def canvasDims: () => cp.Vect
  def pos: cp.Vect
  def scale: Double

  def toTuple = (pos, scale)

  def transform[T](ctx: dom.CanvasRenderingContext2D)(thunk: dom.CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(canvasDims().x/2, canvasDims().y/2)
    ctx.scale(scale, scale)
    ctx.translate(-pos.x, -pos.y)
    thunk(ctx)
    ctx.restore()
  }
}

object Camera{

  class Follow(targetPos: => cp.Vect, widest: cp.Vect, val canvasDims: () => cp.Vect, var scale: Double) extends Camera{
    var pos = new cp.Vect(targetPos.x, targetPos.y)
    def update(dt: Double, keys: Set[Int]) = {
      if (keys(KeyCode.pageUp)) {
        scale = (scale * 1.03)
      }

      if (keys(KeyCode.pageDown)) {
        scale = (scale / 1.03) max ((canvasDims().x / widest.x).toDouble min (canvasDims().y / widest.y))
      }

      if (pos != targetPos){
        pos = targetPos * 0.03 + pos * 0.97
        val x1 = canvasDims().x / 2 / scale
        val y1 = canvasDims().y / 2 / scale
        val xLow = pos.x < x1
        val xHigh = pos.x > widest.x - x1
        val yLow = pos.y < y1
        val yHigh = pos.y > widest.y - y1
        pos = new cp.Vect(
          (xLow, xHigh) match {
            case (false, false) => pos.x
            case (true, false) => x1
            case (false, true) => widest.x - x1
            case (true, true) => widest.x/2
          },
          (yLow, yHigh) match {
            case (false, false) => pos.y
            case (true, false) => y1
            case (false, true) => widest.y - y1
            case (true, true) => widest.y/2
          }
        )
      }
    }

  }
  class Pan(val canvasDims: () => cp.Vect, checkpoints: List[(cp.Vect, Double)], finalCamera: Camera) extends Camera{
    var (aPos, aScale) :: rest = checkpoints
    var fraction = 0.0
    def scaledFraction = (-2 * fraction + 3) * fraction * fraction
    val step = 0.01

    def update(dt: Double, keys: Set[Int]) = {
      finalCamera.update(dt, keys)
      fraction += step
      while(fraction >= 1){
        rest match {
          case Nil =>
            fraction -= step
          case (p, s) :: r =>
            fraction -= 1
            aPos = p
            aScale = s
            rest = r
        }
      }
    }

    def nextTuple = rest match{
      case (p, s) :: r => (p, s)
      case Nil => finalCamera.toTuple
    }

    def scale = {
      aScale * (1-scaledFraction) + nextTuple._2 * scaledFraction
    }

    def pos = {
      aPos * (1-scaledFraction) + nextTuple._1 * scaledFraction
    }
  }
}

