package example.roll

import example.cp.Implicits._
import org.scalajs.dom.extensions._
import scala.scalajs.js
import example.cp
import org.scalajs.dom

class Goal(space: cp.Space, goalElement: dom.Element){
  var goal =
    Form.processElement(goalElement, static = true)(space)(0)

  var won = false
  var text = "Goal"
  goal.shapes.foreach{s =>
    s.setElasticity(0)
    s.setFriction(0)
    s.setCollisionType(1)
  }

  val p = {
    val points  = goal.drawable.asInstanceOf[Drawable.Polygon].points.map(x => x: cp.Vect)
    points.reduce(_ + _) / points.length

  }
  space.addCollisionHandler(1, 1, null, (arb: cp.Arbiter, space: cp.Space) => {
    goal = new Form(
      goal.body,
      goal.shapes,
      goal.drawable,
      Color.Yellow
    )
    text = "Success!\n\nTouch to\nContinue"

    won = true
  }, null)

  def draw(ctx: dom.CanvasRenderingContext2D) = {
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = "20pt arial"
    ctx.lineWidth = 3
    ctx.fillStyle = goal.fillStyle
    ctx.strokeStyle = goal.strokeStyle
    goal.drawable.draw(ctx)
    ctx.fillStyle = Color.Black.toString
    val chunks = text.split("\n")
    for(i <- 0 until chunks.length){
      ctx.fillText(chunks(i), p.x, p.y - 25 * (chunks.length / 2.0 - 0.5 - i))
    }

  }
}
