package example.roll

import org.scalajs.dom
import example.cp
import cp.Implicits._

class Clouds(widest: cp.Vect, view: () => cp.Vect) {
  val cloudImg =
    dom.extensions
      .Image
      .createBase64Svg(scala.js.bundle.apply("CloudIcon.svg").base64)

  class Cloud(var pos: cp.Vect, val vel: Double)
  val clouds = Seq.fill((widest.x * widest.y / 100000).toInt){
    new Cloud(
      widest * (math.random, math.random) * 2,
      math.random
    )
  }
  def update() = {
    for(cloud <- clouds){
      cloud.pos.x += cloud.vel
      cloud.pos.x = (cloud.pos.x + widest.x/2) % (widest.x * 2) - widest.x/2
    }
  }
  def draw(ctx: dom.CanvasRenderingContext2D) = {
    for(cloud <- clouds){
      ctx.drawImage(
        cloudImg,
        cloud.pos.x - cloudImg.width/2,
        cloud.pos.y - cloudImg.height/2
      )
    }
  }
}
