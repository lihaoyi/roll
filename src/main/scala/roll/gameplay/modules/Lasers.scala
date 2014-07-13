package roll.gameplay.modules

import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.cp
import roll.cp.Implicits._
import roll.cp.SegmentQueryInfo
import roll.gameplay.{Layers, Form}
import scala.collection.mutable

case class Beam(start: cp.Vect,
                 end: cp.Vect,
                 var hit: Option[cp.Vect])

class Beams(beamElements: Seq[dom.Element], color: dom.extensions.Color){
  var strokeWidth = 1
  val beams: Seq[Beam] =
    beamElements
      .map{ case (e: dom.SVGLineElement) => Beam((e.x1, e.y1), (e.x2, e.y2), None) }

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(beam <- beams){
      ctx.strokeStyle = color.toString()
      ctx.fillStyle = color.toString()
      strokeWidth += 1
      ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
      val realEnd = beam.hit.fold(beam.end){ hit =>
        ctx.fillCircle(hit.x, hit.y, ctx.lineWidth)
        hit
      }

      ctx.strokePathOpen(beam.start, realEnd)
    }
  }
}
class Lasers(player: Form,
             laserElements: Seq[dom.Element],
             query: (cp.Vect, cp.Vect, js.Number) => SegmentQueryInfo,
             pointQuery: (cp.Vect, js.Number) => cp.Shape,
             kill: => Unit) extends Beams(laserElements, Color.Red){
  println("Laser " + laserElements.length)
  def update() = {
    for (laser <- beams){
      laser.hit = None
      if (pointQuery(laser.start, Layers.Strokes | Layers.DynamicRange) != null) laser.hit = Some(laser.start)
      if (laser.hit == None){
        val res = query(laser.start, laser.end, Layers.Strokes | Layers.DynamicRange)
        if (res != null){
          if (res.shape.getBody == player.body) kill
          laser.hit = Some(laser.start + (laser.end - laser.start) * res.t)
        }
      }
    }
  }
}
class Antigravity(beamElements: Seq[dom.Element],
                 query: (cp.Vect, cp.Vect, js.Number, js.Function3[cp.Shape, Double, cp.Vect, Unit]) => Unit,
                 pointQuery: (cp.Vect, js.Number) => cp.Shape) extends Beams(beamElements, Color.Blue){
  println("Antigravity " + beamElements.length)
  def update() = {
    val hitMap = mutable.Map.empty[cp.Body, List[cp.Vect]]
                            .withDefaultValue(Nil)
    for (laser <- beams){
      laser.hit = None
      if (laser.hit == None){
        val hits = mutable.Buffer.empty[cp.Shape]
        query(
          laser.start,
          laser.end,
          Layers.Strokes | Layers.DynamicRange,
          (s: cp.Shape, d: Double, v: cp.Vect) => hits.append(s)
        )

        for(shape <- hits) {
          val body = shape.getBody()
          if (!body.isStatic()) {
            val d = laser.end - laser.start
            hitMap(body) = d / d.length :: hitMap(body)
          }
        }
      }
    }
    for((body, hits) <- hitMap){
      val cancelGravity = new cp.Vect(0, -400)
      val forwardMotion = hits.reduce(_ + _) / hits.length * 400
      val drag = body.getVel() * -1
      body.applyImpulse(
        (cancelGravity + forwardMotion + drag) * body.m / 60,
        (0, 0)
      )
    }
  }
}