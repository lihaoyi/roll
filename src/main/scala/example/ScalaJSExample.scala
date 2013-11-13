package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import scala.scalajs.extensions._

class Camera(ctx: CanvasRenderingContext2D, pos: => cp.Vect, canvasDims: => cp.Vect, var scale: cp.Vect){

  def update(dt: Double, keys: Set[Int]) = {
    if (keys(KeyCode.pageUp)) scale *= 1.01
    if (keys(KeyCode.pageDown)) scale /= 1.01
  }

  def transform[T](thunk: CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(canvasDims.x/2-pos.x, canvasDims.y/2-pos.y)
    ctx.scale(scale.x, scale.y)
    thunk(ctx)

    ctx.strokeStyle = Color.Red
    ctx.fillStyle = Color.Red

    ctx.restore()
  }
}

class GameHolder(canvas: HTMLCanvasElement, gameMaker: () => Game){

  def bounds = new cp.Vect(canvas.width, canvas.height)

  private[this] val keys = mutable.Set.empty[Int]

  var game: Game = gameMaker()

  val camera = new Camera(
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D],
    pos = game.cameraPos,
    canvasDims = bounds,
    scale = (1, 1)
  )


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
  def cameraPos: cp.Vect
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
}
