package example
package roll

import scala.scalajs.js
import scala.scalajs.extensions._
import example.cp
import example.cp.Implicits._

class Lasers(space: cp.Space, player: Form, laserElement: js.HTMLElement, dead: () => Boolean, kill: () => Unit){
  var strokeWidth = 1

  val lasers: Seq[(cp.Vect, cp.Vect, Ref[Option[cp.Vect]])] =
    laserElement
      .children
      .map{ case (e: js.SVGLineElement) => (
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
  def draw(ctx: js.CanvasRenderingContext2D) = {
    for((start, end, hit) <- lasers){
      strokeWidth += 1
      ctx.lineWidth = (math.sin(strokeWidth / 5) + 1) * 1 + 2
      ctx.strokeStyle = Color.Red.toString()
      ctx.strokePathOpen(
        start,
        hit().getOrElse(end: cp.Vect): cp.Vect
      )
    }
  }
}
