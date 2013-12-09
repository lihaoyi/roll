package example.roll

import org.scalajs.dom
import org.scalajs.dom.extensions._
import example.cp.Implicits._
import example.cp

trait Camera{
  def update(dt: Double, keys: Set[Int])
  def canvasDims: () => cp.Vect
  def widest: cp.Vect
  def pos = {
    def bound(get: cp.Vect => Double, log: Boolean = false) = {
      val w = get(canvasDims()) / 2 / scale
      val low = get(innerPos) < w
      val high = get(innerPos) > get(widest) - w

      if (w * 2 > get(widest)) get(widest) / 2
      else (low, high) match {
        case (false, false) => get(innerPos)
        case (true,  false) => w
        case (false, true)  => get(widest) - w
      }
    }
    
    new cp.Vect(bound(_.x, log = true), bound(_.y))
  }
  def scale: Double
  def innerPos: cp.Vect
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

  class Follow(targetPos: => cp.Vect, val widest: cp.Vect, val canvasDims: () => cp.Vect, var scale: Double) extends Camera{
    var innerPos = new cp.Vect(targetPos.x, targetPos.y)
    def update(dt: Double, keys: Set[Int]) = {
      if (keys(KeyCode.pageUp)) scale = scale * 1.03
      if (keys(KeyCode.pageDown)) scale = scale / 1.03

      scale = scale max ((canvasDims().x / widest.x).toDouble min (canvasDims().y / widest.y))

      if (innerPos != targetPos){
        innerPos = targetPos * 0.03 + innerPos * 0.97
      }
    }
  }

  class Pan(val canvasDims: () => cp.Vect, val widest: cp.Vect, checkpoints: List[(cp.Vect, Double)], finalCamera: Camera) extends Camera{
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

    def innerPos = {
      aPos * (1-scaledFraction) + nextTuple._1 * scaledFraction
    }
  }
}

