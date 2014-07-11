package roll
package gameplay
package modules

import scala.scalajs.js

import roll.cp.Implicits._
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.cp
import roll.gameplay.Layers

class Strokes(space: cp.Space){
  val duration = 150
  val max = 600.0
  var remaining = max
  val regenRate = 2.0
  val delayMax = 60
  var delay = delayMax
  var strokes = Seq.empty[(cp.SegmentShape, Long)]

  var frame = 0L
  var prev: Option[cp.Vect] = None
  def drawStatic(ctx: dom.CanvasRenderingContext2D, w: js.Number, h: js.Number) = {
    ctx.fillStyle = Color.Cyan.toString
    ctx.fillRect(0, h - 10, w * 1.0 * remaining / max, 10)
  }

  var strokeWidth = 1
  def draw(ctx: dom.CanvasRenderingContext2D) = {

    ctx.strokeStyle = Color.Cyan.toString
    strokeWidth += 1
    ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
    strokes.foreach{ case (first, dur) =>
      ctx.strokePathOpen(first.a, first.b)
    }

  }
  def update(touches: Seq[Touch]) = {
    frame = frame + 1
    val (liveStrokes, deadStrokes) = strokes.partition{
      case (s, t) => t + duration > frame
    }
    def hitDynamicShape(p1: cp.Vect, p2: cp.Vect) = {

      val p1p2Hit = Option(space.segmentQueryFirst(p1, p2, Layers.DynamicRange, 0))
      val p2p1Hit = Option(space.segmentQueryFirst(p2, p1, Layers.DynamicRange, 0))
      val p1Hit = space.pointQueryFirst(p1, Layers.DynamicRange, 0) != null
      val p2Hit = space.pointQueryFirst(p2, Layers.DynamicRange, 0) != null
      (p1Hit, p2Hit, p1p2Hit, p2p1Hit)
    }

    def makeSegment(p1: cp.Vect, p2: cp.Vect) = {
      val d = p2 - p1
      if (remaining > 0 && d.length > 0){

        val lengthLeft = math.min(remaining, d.length)

        val p3 = d * lengthLeft / d.length + p1

        remaining -= lengthLeft
        val shape = new cp.SegmentShape(
          space.staticBody,
          (p1.x, p1.y),
          (p3.x, p3.y),
          0
        )
        shape.r = 1
        shape.setFriction(0.6)
        shape.setElasticity(0.1)
        shape.setLayers(Layers.Common | Layers.Strokes)
        prev = Some(p3)
        List(shape)
      }else{
        Nil
      }
    }
    val newStrokes = touches.flatMap{
      case Touch.Down(p) =>
        prev = Some(p)
        Nil
      case Touch.Move(p) if remaining > 0 && prev.isDefined =>
        val p1 = prev.get
        delay = delayMax
        hitDynamicShape(p1, p) match{
          case (false, false, None, None) => makeSegment(p1, p)
          case (true, false, _, Some(t)) => makeSegment(p + (p1 - p) * t.t, p) // start collides
          case (false, true, Some(t), _) => makeSegment(p1, p1 + (p - p1) * t.t) // end collides
          case (true, true, _, _) => Nil // both ends collide
          case (false, false, Some(t1), Some(t2)) =>
            makeSegment(p1, p1 + (p - p1) * t1.t) ++ makeSegment(p + (p1 - p) * t2.t, p) // middle collides
          case _ => ??? //this should never happen
        }
      case Touch.Up(p) =>
        prev = None
        Nil
      case p => Nil
    }

    newStrokes.foreach(space.addShape)

    if (prev.isEmpty && remaining < max) {
      if (delay > 0) delay -= 1
      else remaining += regenRate
    }

    deadStrokes.map(_._1).foreach(space.removeShape)
    strokes = newStrokes.map(_ -> frame) ++ liveStrokes
  }
}