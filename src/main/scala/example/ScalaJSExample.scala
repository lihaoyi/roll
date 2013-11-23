package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js

class Camera(targetPos: => cp.Vect, canvasDims: => cp.Vect, var scale: cp.Vect){
  var pos = new cp.Vect(targetPos.x, targetPos.y)
  def update(dt: Double, keys: Set[Int]) = {
    if (keys(KeyCode.pageUp)) scale *= 1.03
    if (keys(KeyCode.pageDown)) scale /= 1.03

    if (pos != targetPos){
      pos = targetPos * 0.03 + pos * 0.97
    }
  }

  def transform[T](ctx: CanvasRenderingContext2D)(thunk: CanvasRenderingContext2D => T) = {
    ctx.save()
    ctx.translate(canvasDims.x/2, canvasDims.y/2)
    ctx.scale(scale.x, scale.y)

    ctx.translate(-pos.x, -pos.y)
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
    targetPos = game.cameraPos,
    canvasDims = bounds,
    scale = (1, 1)
  )

  var prev: cp.Vect = null
  var lines: List[(cp.Vect, cp.Vect)] = Nil

  def screenToWorld(p: cp.Vect) = p - bounds/2 + camera.pos

  def event(e: Event): Unit = e match{
    case e: KeyboardEvent if e.`type` == "keydown" =>  keys.add(e.keyCode.toInt)
    case e: KeyboardEvent if e.`type` == "keyup" =>  keys.remove(e.keyCode.toInt)
    case e: PointerEvent if e.`type` == "pointerdown" => prev = screenToWorld(new cp.Vect(e.x, e.y))
    case e: PointerEvent if e.`type` == "pointermove" =>
      val next = screenToWorld(new cp.Vect(e.x, e.y))
      if (prev != null && (next - prev).length > 3){

        lines = (prev, next) :: lines
        prev = next
      }
    case e: PointerEvent if e.`type` == "pointerup" => prev = null
    case e: PointerEvent if e.`type` == "pointerout" => prev = null
    case e: PointerEvent if e.`type` == "pointerleave" => prev = null
    case _ => println("Unknown event " + e.`type`)
  }

  var active = false

  var now = Calc(Date.now() / 1000)
  val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  def update() = {
    canvas.width = js.globals.innerWidth
    canvas.height = js.globals.innerHeight
    val oldNow = now()
    now.recalc()
    camera.update(now() - oldNow, keys.toSet)
    game.update(keys.toSet, lines, prev != null)
    lines = Nil
    canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    camera.transform(ctx){ ctx =>
      game.draw(ctx)
    }

    game.drawStatic(ctx, canvas.width.toInt, canvas.height.toInt)
  }
}

abstract class Game{
  var result: Option[String] = None
  def update(keys: Set[Int], lines: Seq[(cp.Vect, cp.Vect)], touching: scala.Boolean): Unit
  def draw(ctx: CanvasRenderingContext2D): Unit
  def drawStatic(ctx: CanvasRenderingContext2D, w: Int, h: Int): Unit
  def cameraPos: cp.Vect
}

object ScalaJSExample {
  def main(): Unit = {
    val canvas =
      js.globals
        .window
        .document
        .getElementById("screen")
        .asInstanceOf[HTMLCanvasElement]

    val ribbonGame = Calc(new GameHolder(canvas, Roll))

    Seq("keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave").foreach(s =>
      js.globals.window.document.body.addEventListener(s, (e: Event) => ribbonGame().event(e))
    )
    js.globals.setInterval(() => ribbonGame().update(), 10)
  }
}
