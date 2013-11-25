package example

import scala.scalajs.js
import scala.scalajs.extensions.Color
import example.cp.Implicits._
import scala.scalajs.extensions._

class Strokes(space: cp.Space){
  var duration = 1500
  var max = 600.0
  var remaining = max
  var regenRate = 2.0
  var delayMax = 60
  var delay = delayMax
  var strokes = Seq.empty[(cp.SegmentShape, Long)]


  def drawStatic(ctx: js.CanvasRenderingContext2D, w: Int, h: Int) = {
    ctx.fillStyle = Color.Cyan.toString
    ctx.fillRect(0, h - 10, w * 1.0 * remaining / max, 10)
  }

  var strokeWidth = 1
  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.strokeStyle = Color.Cyan.toString
    strokeWidth += 1
    ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
    strokes.foreach{ case (first, dur) =>
      ctx.strokePathOpen(first.a, first.b)
    }
  }
  def update(lines: Seq[(cp.Vect, cp.Vect)], touching: Boolean) = {
    val (liveStrokes, deadStrokes) = strokes.partition{
      case (s, t) => t + duration > System.currentTimeMillis()
    }
    def hitDynamicShape(p1: cp.Vect, p2: cp.Vect) = {
      val shapes = collection.mutable.Buffer.empty[cp.Shape]
      space.segmentQuery(p1, p2, ~0, 0, {(s: cp.Shape, t: js.Number , n: cp.Vect) => shapes += s; ()})
      space.pointQuery(p1, ~0, 0, {(s: cp.Shape) => shapes += s; ()})
      space.pointQuery(p2, ~0, 0, {(s: cp.Shape) => shapes += s; ()})
      shapes.exists(!_.getBody().isStatic())
    }
    val newStrokes = for {
      (p1, p2) <- lines
      if remaining > 0
      d = p2 - p1
      lengthLeft = math.min(remaining, d.length)
      p3 = d * lengthLeft / d.length + p1
      if !hitDynamicShape(p1, p3)
    } yield {
      delay = delayMax

      remaining -= lengthLeft
      val shape = new cp.SegmentShape(
        space.staticBody,
        (p1.x, p1.y),
        (p3.x, p3.y),
        0
      )
      space.addShape(shape)

      shape.setFriction(0.6)
      shape.setElasticity(0.1)
      shape -> System.currentTimeMillis()
    }
    if (!touching && remaining < max) {
      if (delay > 0) delay -= 1
      else remaining += regenRate
    }

    deadStrokes.map(_._1).foreach(space.removeShape)
    strokes = newStrokes ++ liveStrokes
  }
}