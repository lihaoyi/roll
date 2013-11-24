package example

import example.cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js

class Goal(space: cp.Space, goalElement: js.Element){
  var goal =
    Form.processElement(goalElement, static = true)(space)

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
    text = "Success!"
  }, null)

  def draw(ctx: js.CanvasRenderingContext2D) = {
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = "20pt arial"
    ctx.lineWidth = 3
    ctx.fillStyle = goal.fillStyle
    ctx.strokeStyle = goal.strokeStyle
    goal.drawable.draw(ctx)
    ctx.fillStyle = Color.Black.toString
    ctx.fillText(text, p.x, p.y)
  }
}
