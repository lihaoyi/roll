package roll.gameplay.modules

import roll.cp.Implicits._
import org.scalajs.dom.extensions._
import roll.cp
import org.scalajs.dom
import roll.gameplay.{Drawable, Form}

class Goal(var goal: Form, exit: () => Unit){

  var countDown = 1.0
  def started = countDown == 0
  var won = false
  var text = "Goal"
  goal.shapes.foreach{s =>
    s.setElasticity(0)
    s.setFriction(0)
    s.setCollisionType(1)
  }

  val p = {
    val points  = goal.drawable.cast[Drawable.Polygon].points.map(x => x: cp.Vect)
    points.reduce(_ + _) / points.length

  }

  def hit() = {
    goal = new Form(
      goal.body,
      goal.shapes,
      goal.drawable,
      Color.Yellow
    )
    text = "Success!\n\nTouch to\nContinue"
    won = true
  }
  def update() = {
    if (won){
      countDown += 0.04
      if (countDown >= 1){
        exit()
      }
    }else{
      countDown -= 0.04
      if (countDown <= 0) {
        countDown = 0
      }
    }

  }
  def drawFade(ctx: dom.CanvasRenderingContext2D) = {
    ctx.fillStyle = s"rgba(0, 0, 0, $countDown)"
    ctx.fillRect(0, 0, 2000, 1000)
  }
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    ctx.textAlign = "center"
    ctx.textBaseline = "middle"
    ctx.font = "20pt arial"
    ctx.lineWidth = 3
    ctx.fillStyle = goal.fillStyle.toString
    ctx.strokeStyle = goal.strokeStyle.toString
    goal.drawable.draw(ctx)
    ctx.fillStyle = Color.Black.toString
    val chunks = text.split("\n")

    for(i <- 0 until chunks.length){
      ctx.fillText(chunks(i), p.x, p.y - 25 * (chunks.length / 2.0 - 0.5 - i))
    }
  }
}
