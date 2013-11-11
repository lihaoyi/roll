package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._

class Camera(ctx: CanvasRenderingContext2D, var pos: cp.Vect, var scale: cp.Vect){

  val offsets = Map[Int, cp.Vect](
    KeyCode.right -> (-1, 0),
    KeyCode.left -> (1, 0),
    KeyCode.up -> (0, 1),
    KeyCode.down -> (0, -1)
  )
  var speed = 300

  def update(dt: Double, keys: Set[Int]) = {
    for{
      (key, pt) <- offsets
      if keys(key)
    }{
      pos += pt * dt * speed
    }
    if (keys(KeyCode.pageUp)) scale *= 1.01
    if (keys(KeyCode.pageDown)) scale /= 1.01
  }

  def transform[T](w: Double, h: Double)(thunk: CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(pos.x, pos.y)
    ctx.translate(w, h)
    ctx.scale(scale.x, scale.y)
    ctx.translate(-w, -h)
    thunk(ctx)
    ctx.restore()
  }
}
class GameHolder(canvas: HTMLCanvasElement, gameMaker: () => Game){

  val bounds = Calc(new cp.Vect(canvas.width, canvas.height))

  private[this] val keys = mutable.Set.empty[Int]

  val camera = new Camera(
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D],
    pos = (0, 0),
    scale = (1, 1)
  )
  var game: Game = gameMaker()

  canvas.onkeydown = {(e: KeyboardEvent) =>
    keys.add(e.keyCode.toInt)
  }
  canvas.onkeyup = {(e: KeyboardEvent) =>
    keys.remove(e.keyCode.toInt)
  }

  var active = false

  var now = Calc(Date.now() / 1000)

  def update() = {
    canvas.width = JsGlobals.innerWidth
    canvas.height = JsGlobals.innerHeight
    val oldNow = now()
    now.recalc()
    camera.update(now() - oldNow, keys.toSet)
    game.update(keys.toSet)

    camera.transform(canvas.width/2, canvas.height/2){ ctx =>
      game.draw(ctx)
    }
  }

}

abstract class Game{
  var result: Option[String] = None
  def update(keys: Set[Int]): Unit
  def draw(ctx: CanvasRenderingContext2D): Unit
}

object ScalaJSExample {
  def main(): Unit = {
    val canvas =
      JsGlobals
        .window
        .document
        .getElementById("screen")
        .asInstanceOf[HTMLCanvasElement]

    val ribbonGame = Calc(new GameHolder(canvas, Roll))
    canvas.onfocus = {(e: FocusEvent) =>
      ribbonGame.recalc()
    }

    JsGlobals.setInterval(() => ribbonGame().update(), 15)
  }

  implicit class pimpedContext(val ctx: CanvasRenderingContext2D){
    def fillCircle(x: Double, y: Double, r: Double) = {
      ctx.beginPath()
      ctx.arc(x, y, r, 0, math.Pi * 2)
      ctx.fill()
    }
    def strokePath(points: cp.Vect*) = {
      ctx.beginPath()
      ctx.moveTo(points.last.x, points.last.y)
      for(p <- points){
        ctx.lineTo(p.x, p.y)
      }
      ctx.stroke()
    }
  }
}
