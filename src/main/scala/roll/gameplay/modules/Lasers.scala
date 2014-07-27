package roll.gameplay.modules

import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.{Xml, cp}
import roll.cp.Implicits._
import roll.cp.SegmentQueryInfo
import roll.gameplay.{Drawable, Layers, Form}
import scala.collection.mutable
import java.util.Random

case class Beam(start: cp.Vect,
                 end: cp.Vect,
                 var hit: Option[cp.Vect],
                 var strokeWidth: Double = 1.0,
                 var spots: List[Double] = Nil)

class Beams(beamLines: Seq[Xml.Line], color: dom.extensions.Color){

  val beams: Seq[Beam] = for{
    Xml.Line(x1, y1, x2, y2, misc) <- beamLines
  } yield {
    Beam((x1, y1), (x2, y2), None)
  }

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(beam <- beams){
      ctx.strokeStyle = color.toString()
      ctx.fillStyle = color.toString()
      beam.strokeWidth += 1
      if(scala.util.Random.nextFloat() > 0.9 && !beam.spots.headOption.exists(_ < 200)) beam.spots ::= 0

      ctx.lineWidth = (math.sin(beam.strokeWidth / 5) + 1) * 1 + 2
      val realEnd = beam.hit.fold(beam.end){ hit =>
        ctx.fillCircle(hit.x, hit.y, ctx.lineWidth)
        hit
      }

      beam.spots =
        beam.spots
            .map(_ + 10)
            .filter(_ < (beam.start - realEnd).length)


      ctx.strokePathOpen(beam.start, realEnd)

      for(spot <- beam.spots){
        val d = beam.end - beam.start
        val pos = d / d.length * spot + beam.start
        ctx.fillCircle(pos.x, pos.y, ctx.lineWidth)
      }
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

