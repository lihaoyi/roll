package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._


class Camera(ctx: CanvasRenderingContext2D, var pos: Point, var scale: Point){

  val offsets = Map(
    KeyCode.right -> Point(-1, 0),
    KeyCode.left -> Point(1, 0),
    KeyCode.up -> Point(0, 1),
    KeyCode.down -> Point(0, -1)
  )
  var speed = 300

  def update(dt: Double, keys: Set[Int]) = for{
    (key, pt) <- offsets
    if keys(key)
  }{
    pos += pt * dt * speed
  }

  def transform[T](thunk: CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(pos.x, pos.y)
    ctx.scale(scale.x, scale.y)
    thunk(ctx)
    ctx.restore()
  }
}
class GameHolder(canvas: HTMLCanvasElement, gameMaker: () => Game){

  val bounds = Calc(Point(canvas.width, canvas.height))

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

    camera.transform{ ctx =>
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
    val canvas = JsGlobals.window.document.getElementById("screen").asInstanceOf[HTMLCanvasElement]
    val ribbonGame = Calc(new GameHolder(canvas, Tetris))
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
    def strokePath(points: Point*) = {

      ctx.beginPath()
      ctx.moveTo(points.last.x, points.last.y)
      for(p <- points){
        ctx.lineTo(p.x, p.y)
      }
      ctx.stroke()
    }
  }
}
