package example.roll

import scala.scalajs.extensions._
import scala.scalajs.js
import example.roll.Form
import example.cp
import example.cp.Implicits._

class Player(space: cp.Space, playerElement: js.Element) {
  var dead = 0.0
  val form = Form.processElement(playerElement, static = false)(space)
  form.shapes(0).setCollisionType(1)
  val startPos = (form.body.getPos.x, form.body.getPos.y)
  def draw(ctx: js.CanvasRenderingContext2D) = {
    if (dead > 0) ctx.globalAlpha = dead
    Roll.draw(ctx, form)
    ctx.globalAlpha = 1.0
  }
  def update(keys: Set[Int]) = {
    if (dead > 0.0) {
      dead -= 0.05
      if (dead < 0){
        dead = 0.0
        form.body.setPos(startPos)
        form.body.setVel((0, 0))
        form.body.setAngVel(0)
      }
    }else{
      val baseT = 0.45
      val maxW = 25
      val decay = (maxW - baseT) / maxW
      if (keys(KeyCode.left)) {
        form.body.w = (form.body.w - baseT) * decay
      }
      if (keys(KeyCode.right)){
        form.body.w = (form.body.w + baseT) * decay
      }
    }
  }
}
