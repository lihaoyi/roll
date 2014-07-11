package roll

import org.scalajs.dom
import roll.gameplay.Form
import scala.concurrent.Future

/**
 * Created by haoyi on 7/10/14.
 */
object Util {
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

sealed trait Touch
object Touch{
  case class Down(p: cp.Vect) extends Touch
  case class Move(p: cp.Vect) extends Touch
  case class Up(p: cp.Vect) extends Touch
}
/**
 * Used to mark a Future as a task which returns Unit, making
 * sure to print the error and stack trace if it fails.
 */
object task{
  def *[T](f: Future[T])(implicit ec: scala.concurrent.ExecutionContext) = {
    f.map(_ => ()).recover{ case e =>
      e.printStackTrace()
    }
  }
}