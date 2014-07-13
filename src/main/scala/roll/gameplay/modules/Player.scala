package roll.gameplay.modules

import org.scalajs.dom.extensions._
import roll.gameplay.Form
import roll.{cp, Util}
import roll.cp.Implicits._
import org.scalajs.dom

class Player(val form: Form, widest: cp.Vect) {
  var dead = 0.0

  form.shapes(0).setCollisionType(1)
  //form.body.setMoment(form.body.i / 2)
  val startPos = (form.body.getPos.x, form.body.getPos.y)
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    if (dead > 0) ctx.globalAlpha = dead
    Util.draw(ctx, form)
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
      val pos = form.body.getPos()
      if (pos.x < 0 || pos.y < 0 || pos.x > widest.x || pos.y > widest.y){
        dead = 1.0
      }
      val baseT = 0.45
      val maxW = 30
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
