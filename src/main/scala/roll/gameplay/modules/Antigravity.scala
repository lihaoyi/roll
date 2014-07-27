package roll.gameplay.modules

import roll.cp
import roll.gameplay.{Layers, Drawable}
import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.extensions._
import roll.cp.Implicits._
import scala.collection.mutable

case class Field(center: cp.Vect,
                 drawable: Drawable,
                 shape: cp.Shape,
                 dir: cp.Vect,
                 area: Double,
                 var sparkles: List[(cp.Vect)] = Nil){

}

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

      val bb = field.shape.getBB()
      ctx.strokeStyle = s"rgba(128, $g, $b, 0.2)"
      ctx.fillStyle = s"rgba(128, $g, $b, 0.2)"

      val idealCount = math.abs((bb.l - bb.r) * (bb.t - bb.b)  / 10000).toInt

      for{
        x <- 0 until (idealCount - field.sparkles.length)
        if scala.util.Random.nextFloat() > 0.95
      }{
        def rand = util.Random.nextDouble()

        // Random points along the four edges of the bounding box
        def randT = (rand * (bb.r - bb.l) + bb.l, bb.t)
        def randL = (bb.l, rand * (bb.t - bb.b) + bb.b)
        def randB = (rand * (bb.r - bb.l) + bb.l, bb.b)
        def randR = (bb.r, rand * (bb.t - bb.b) + bb.b)

        val (absY, absX) = (math.abs(field.dir.y), math.abs(field.dir.x))

        val pt: cp.Vect =
          if (rand < absX / (absX + absY))
            if (field.dir.x < 0)  randR
            else randL
          else
            if (field.dir.y < 0) randT
            else randB

        field.sparkles = pt :: field.sparkles
      }
      field.drawable.draw(ctx)
      field.sparkles =
        field.sparkles
             .map(_ + field.dir * 4)
             .filter(p => p.within((bb.l, bb.t), (bb.r, bb.b)))

      ctx.fillStyle = s"rgba(255, 255, 255, 0.8)"
      for{
        spot <- field.sparkles
        if field.shape.pointQuery(spot).isDefined
      }{
        ctx.fillCircle(spot.x, spot.y, 4)
      }
    }
  }

  def update() = {
    val hitMap = mutable.Map.empty[cp.Body, List[cp.Vect]]
                            .withDefaultValue(Nil)

    for(field <- fields){
      field.shape.layers = Layers.DynamicRange
      query(field.shape, (s, _) => hitMap(s.getBody()) ::= field.dir)
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
