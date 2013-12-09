package example.roll

import org.scalajs.dom
import example.cp


class Clouds(widest: cp.Vect, view: () => cp.Vect) {
  val cloudImg =
    dom.extensions
      .Image
      .createBase64Svg(scala.js.bundle.apply("CloudIcon.svg").base64)

  class Cloud(var pos: cp.Vect, val vel: Double)
  val clouds = Seq.fill(50){
    new Cloud(
      new cp.Vect(
        (widest.x + view().x) * math.random,
        (widest.y + view().y) * math.random
      ),
      math.random
    )
  }
  def update() = {
    for(cloud <- clouds){
      cloud.pos.x += cloud.vel
      cloud.pos.x %= (widest.x + view().x)
    }
  }
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(cloud <- clouds){
      ctx.drawImage(
        cloudImg,
        cloud.pos.x - cloudImg.width/2 - view().x/2,
        cloud.pos.y - cloudImg.height/2 - view().y/2
      )
    }
  }
}
