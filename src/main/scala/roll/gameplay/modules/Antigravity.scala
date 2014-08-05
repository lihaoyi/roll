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
                 acceleration: Double,
                 drag: Double,
                 dir: cp.Vect){
  val bb = shape.getBB()
  val idealCount = math.abs((bb.l - bb.r) * (bb.t - bb.b)  / 10000).toInt
  val sparkles: Array[cp.Vect] = new Array(idealCount)
}

class Antigravity(fields: Seq[Field],
                  query: (cp.Shape, Function2[cp.Shape, js.Any, Unit]) => Unit,
                  pointQuery: (cp.Vect, js.Number) => cp.Shape){

  def rand = util.Random.nextDouble()
  for(field <- fields){
    import field._
    (0 until idealCount).foreach{i =>
      sparkles(i) = new cp.Vect(rand * (bb.l - bb.r) + bb.r, rand * (bb.t - bb.b) + bb.b)
    }
  }
  var strokeWidth = 0.0
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    ctx.fill()
    strokeWidth += 0.1
    val base = 240
    val rest = 255 - base
    val g = (base + rest * Math.sin(strokeWidth)).toInt
    val b = (base - rest * Math.sin(strokeWidth)).toInt
    for(field <- fields){
      ctx.strokeStyle = s"rgba(128, $g, $b, 0.2)"
      ctx.fillStyle = s"rgba(128, $g, $b, 0.2)"

      import field._

      field.drawable.draw(ctx)
      var i = sparkles.length - 1
      while(i >= 0){
        sparkles(i).x += field.dir.x * 4
        sparkles(i).y += field.dir.y * 4

        if (!sparkles(i).within((bb.l, bb.t), (bb.r, bb.b))){
          // Random points along the four edges of the bounding box
          def randT = new cp.Vect(rand * (bb.r - bb.l) + bb.l, bb.t)
          def randL = new cp.Vect(bb.l, rand * (bb.t - bb.b) + bb.b)
          def randB = new cp.Vect(rand * (bb.r - bb.l) + bb.l, bb.b)
          def randR = new cp.Vect(bb.r, rand * (bb.t - bb.b) + bb.b)

          // Some clever math to distribute the new sparkles around the
          // two up-stream edges of the bounding box in a way that makes
          // the distribution of sparkles uniform while avoiding singularities
          val (absY, absX) = (math.abs(field.dir.y), math.abs(field.dir.x))

          sparkles(i) = if (rand < absX / (absX + absY)) {
            if (field.dir.x < 0) randR
            else randL
          }else {
            if (field.dir.y < 0) randT
            else randB
          }
        }
        i -= 1
      }

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
    val hitMap = mutable.Map.empty[cp.Body, List[Field]]
                            .withDefaultValue(Nil)

    for(field <- fields){
      field.shape.layers = Layers.DynamicRange
      query(field.shape, (s, _) => hitMap(s.getBody()) ::= field)
      field.shape.layers = Layers.Fields
    }
    for((body, fields) <- hitMap){
      val cancelGravity = new cp.Vect(0, -400)
      val forwardMotion = fields.map(x => x.dir * x.acceleration).reduce(_ + _) / fields.length * 400
      def dragEquation(v: cp.Vect, linearDrag: Double, quadraticDrag: Double = 0.00015) = {
        v * -linearDrag - v * v.length * quadraticDrag
      }
      val drag = fields.map(f => dragEquation(body.getVel(), f.drag)).reduce(_ + _) / fields.length
      body.applyImpulse(
        (cancelGravity + forwardMotion + drag) * body.m / 60,
        (0, 0)
      )
    }
  }
}
