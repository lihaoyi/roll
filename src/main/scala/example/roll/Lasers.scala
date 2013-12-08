package example
package roll

import scala.scalajs.js
import org.scalajs.dom.extensions._
import org.scalajs.dom
import example.cp
import example.cp.Implicits._

class Lasers(space: cp.Space, player: Form, laserElement: dom.HTMLElement, dead: () => Boolean, kill: () => Unit){
  var strokeWidth = 1

  val lasers: Seq[(cp.Vect, cp.Vect, Ref[Option[cp.Vect]])] =
    laserElement
      .children
      .map{ case (e: dom.SVGLineElement) => (
        new cp.Vect(e.x1.baseVal.value, e.y1.baseVal.value),
        new cp.Vect(e.x2.baseVal.value, e.y2.baseVal.value),
        Ref[Option[cp.Vect]](None)
      )}

  def update() = {
    for ((start, end, hit) <- lasers){
      hit() = None
      space.segmentQuery(start, end, ~1, 0, (shape: cp.Shape, t: js.Number, n: cp.Vect) => {
        val body = shape.getBody()
        if (hit() == None && body == player.body && !dead()) kill()
        if (!(body.isStatic: Boolean) && hit() == None && body != player.body){
          hit() = Some(start + (end - start) * t)
        }
      })
    }
  }
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for((start, end, hit) <- lasers){
      ctx.strokeStyle = Color.Red.toString()
      ctx.fillStyle = Color.Red.toString()
      strokeWidth += 1
      ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
      val realEnd = hit() match{
        case None => end
        case Some(hit) =>
          ctx.fillCircle(hit.x, hit.y, ctx.lineWidth)
          hit
      }

      ctx.strokePathOpen(start, realEnd)
    }
  }
}
