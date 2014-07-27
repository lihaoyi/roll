package roll.gameplay.modules

import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.{Xml, cp}
import roll.cp.Implicits._
import roll.cp.SegmentQueryInfo
import roll.gameplay.{Drawable, Layers, Form}
import scala.collection.mutable

case class Beam(start: cp.Vect,
                 end: cp.Vect,
                 var hit: Option[cp.Vect])

class Beams(beamLines: Seq[Xml.Line], color: dom.extensions.Color){
  var strokeWidth = 1
  val beams: Seq[Beam] = for{
    Xml.Line(x1, y1, x2, y2, misc) <- beamLines
  } yield {
    Beam((x1, y1), (x2, y2), None)
  }



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
             laserElements: Seq[Xml.Line],
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

case class Field(center: cp.Vect, drawable: Drawable, shape: cp.Shape, direction: cp.Vect)

class Antigravity(fields: Seq[Field],
                 query: (cp.Shape, Function2[cp.Shape, js.Any, Unit]) => Unit,
                 pointQuery: (cp.Vect, js.Number) => cp.Shape){

  var strokeWidth = 0.0
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    strokeWidth += 0.1
    val base = 240
    val rest = 255 - base
    val g = (base + rest * Math.sin(strokeWidth)).toInt
    val b = (base - rest * Math.sin(strokeWidth)).toInt
    for(field <- fields){
      ctx.strokeStyle = s"rgba(0, $g, $b, 0.5)"
      ctx.fillStyle = s"rgba(0, $g, $b, 0.5)"
      field.drawable.draw(ctx)
    }
  }

  def update() = {
    val hitMap = mutable.Map.empty[cp.Body, List[cp.Vect]]
                            .withDefaultValue(Nil)

    for(field <- fields){
      field.shape.layers = Layers.DynamicRange
      query(field.shape, (s, _) => hitMap(s.getBody()) ::= field.direction)
      field.shape.layers = Layers.Fields
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