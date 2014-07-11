package roll.gameplay.modules

import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.cp
import roll.cp.Implicits._
import roll.cp.SegmentQueryInfo
import roll.gameplay.{Layers, Form}

class Lasers(player: Form,
             laserElement: dom.HTMLElement,
             query: (cp.Vect, cp.Vect, js.Number) => SegmentQueryInfo,
             pointQuery: (cp.Vect, js.Number) => cp.Shape,
             kill: => Unit){
  var strokeWidth = 1
  case class Laser(start: cp.Vect,
                   end: cp.Vect,
                   var hit: Option[cp.Vect])
  val lasers: Seq[Laser] =
    laserElement
      .children
      .map{ case (e: dom.SVGLineElement) =>
        new Laser((e.x1, e.y1), (e.x2, e.y2), None)
      }

  def update() = {
    for (laser <- lasers){
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

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(laser <- lasers){
      ctx.strokeStyle = Color.Red.toString()
      ctx.fillStyle = Color.Red.toString()
      strokeWidth += 1
      ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
      val realEnd = laser.hit.fold(laser.end){ hit =>
        ctx.fillCircle(hit.x, hit.y, ctx.lineWidth)
        hit
      }

      ctx.strokePathOpen(laser.start, realEnd)
    }
  }
}
