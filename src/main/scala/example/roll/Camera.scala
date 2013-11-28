package example.roll

import org.scalajs.dom
import org.scalajs.dom.extensions._
import example.cp.Implicits._
import example.cp

trait Camera{
  def update(dt: Double, keys: Set[Int])
  def canvasDims: () => cp.Vect
  def pos: cp.Vect
  def scale: cp.Vect

  def toTuple = (pos, scale)

  def transform[T](ctx: dom.CanvasRenderingContext2D)(thunk: dom.CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(canvasDims().x/2, canvasDims().y/2)
    ctx.scale(scale.x, scale.y)
    ctx.translate(-pos.x, -pos.y)
    thunk(ctx)
    ctx.restore()
  }
}

object Camera{

  class Follow(targetPos: => cp.Vect, val canvasDims: () => cp.Vect, var scale: cp.Vect) extends Camera{
    var pos = new cp.Vect(targetPos.x, targetPos.y)
    def update(dt: Double, keys: Set[Int]) = {
      if (keys(KeyCode.pageUp)) scale *= 1.03
      if (keys(KeyCode.pageDown)) scale /= 1.03

      if (pos != targetPos){
        pos = targetPos * 0.03 + pos * 0.97
      }
    }

  }
  class Pan(val canvasDims: () => cp.Vect, checkpoints: List[(cp.Vect, cp.Vect)], finalCamera: Camera) extends Camera{
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

