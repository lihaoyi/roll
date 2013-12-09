package example
package roll
import collection.mutable
import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import example.cp
import example.cp.Implicits._

class Lasers(space: cp.Space, player: Form, laserElement: dom.HTMLElement, dead: () => Boolean, kill: () => Unit){
  var strokeWidth = 1
  case class Laser(start: cp.Vect,
                   end: cp.Vect,
                   ignored: Set[cp.Shape],
                   var hit: Option[(cp.Vect, cp.Body)])
  val lasers: Seq[Laser] =
    laserElement
      .children
      .map{ case (e: dom.SVGLineElement) =>
        val start = new cp.Vect(e.x1.baseVal.value, e.y1.baseVal.value)
        val end = new cp.Vect(e.x2.baseVal.value, e.y2.baseVal.value)
        val hits = mutable.Buffer.empty[cp.Shape]
        space.segmentQuery(start, end, ~1, 0, (shape: cp.Shape, t: js.Number, n: cp.Vect) => {
          if (shape.getBody().isStatic()) hits.append(shape)
        })

        new Laser(start, end, hits.toSet, None)
      }

  def update() = {
    for (laser <- lasers){
      laser.hit = None
      space.segmentQuery(laser.start, laser.end, ~1, 0, (shape: cp.Shape, t: js.Number, n: cp.Vect) => {
        val body = shape.getBody()
        if (!laser.ignored.contains(shape) && laser.hit == None){
          if(body == player.body) {
            if (!dead()) kill()
          }else {
            laser.hit = Some((laser.start + (laser.end - laser.start) * t, body))
          }
        }
      })
    }
  }

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(laser <- lasers){
      ctx.strokeStyle = Color.Red.toString()
      ctx.fillStyle = Color.Red.toString()
      strokeWidth += 1
      ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
      val realEnd = laser.hit match{
        case None => laser.end
        case Some(hit) =>
          ctx.fillCircle(hit._1.x, hit._1.y, ctx.lineWidth)
          hit._1
      }

      ctx.strokePathOpen(laser.start, realEnd)
    }
  }
}
