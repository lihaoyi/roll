package roll

import org.scalajs.dom
import roll.gameplay.Form

/**
 * Created by haoyi on 7/10/14.
 */
object Drawer {
  def draw(ctx: dom.CanvasRenderingContext2D, form: Form) = {
    val body = form.body
    ctx.save()
    ctx.lineWidth = 3
    ctx.strokeStyle = form.strokeStyle.toString
    ctx.fillStyle = form.fillStyle.toString
    ctx.translate(
      body.getPos().x,
      body.getPos().y
    )

    ctx.rotate(body.a)
    form.drawable.draw(ctx)
    ctx.restore()
  }
}


case class Input(keys: Set[Int],
                 touches: Seq[Touch],
                 screenSize: cp.Vect,
                 painter: dom.CanvasRenderingContext2D)

sealed trait Touch
object Touch{
  case class Down(p: cp.Vect) extends Touch
  case class Move(p: cp.Vect) extends Touch
  case class Up(p: cp.Vect) extends Touch
}

trait Result
object Result{
  case object Next extends Result
  case object Reset extends Result
}
