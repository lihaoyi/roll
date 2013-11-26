package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js
import example.roll.{Roll, Camera}


class GameHolder(canvas: HTMLCanvasElement, gameMaker: () => Game){
  js.globals.console.log(canvas)
  def bounds = new cp.Vect(canvas.width, canvas.height)

  private[this] val keys = mutable.Set.empty[Int]

  var game: Game = gameMaker()
  val scale = math.min(js.globals.innerWidth / game.widest.x, js.globals.innerHeight / game.widest.y)
  var camera: Camera = new Camera.Pan(
    canvasDims = () => bounds,
    checkpoints = List(
      (game.startCameraPos, new cp.Vect(1, 1)),
      (game.widest / 2, new cp.Vect(1, 1) * scale)
    ),
    finalCamera = new Camera.Follow(
      game.cameraPos,
      () => bounds,
      (1, 1)
    )
  )

  var prev: cp.Vect = null
  var lines: List[(cp.Vect, cp.Vect)] = Nil

  def screenToWorld(p: cp.Vect) = p - bounds/2 + camera.pos

  def event(e: Event): Unit = (e, e.`type`.toString) match{
    case (e: KeyboardEvent, "keydown") =>  keys.add(e.keyCode.toInt)
    case (e: KeyboardEvent, "keyup") =>  keys.remove(e.keyCode.toInt)
    case (e: PointerEvent, "pointerdown") =>

      prev = screenToWorld(new cp.Vect(e.clientX, e.clientY))
      
    case (e: PointerEvent, "pointermove") =>
      val next = screenToWorld(new cp.Vect(e.clientX, e.clientY))
      if (prev != null && (next - prev).length > 3){
        lines = (prev, next) :: lines
        prev = next
      }
    case (_, "pointerup") => prev = null
    case (_, "pointerout") => prev = null
    case (_, "pointerleave") => prev = null
    case _ => println("Unknown event " + e.`type`)
  }

  var active = false

  var now = Calc(Date.now() / 1000)
  val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  def update() = {
    if (canvas.width != js.globals.innerWidth) canvas.width = js.globals.innerWidth
    if (canvas.height != js.globals.innerHeight) canvas.height = js.globals.innerHeight
    val oldNow = now()
    now.recalc()
    camera.update(now() - oldNow, keys.toSet)
    game.update(keys.toSet, lines, prev != null)

    lines = Nil

    ctx.fillStyle = Color(128, 128, 128).toString
    ctx.fillRect(0, 0, canvas.width, canvas.height)

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
  def startCameraPos: cp.Vect
  def widest: cp.Vect
}

object ScalaJSExample {
  def main(): Unit = {
    val canvas =
      js.globals
        .window
        .document
        .getElementById("screen")
        .asInstanceOf[HTMLCanvasElement]

    val ribbonGame = Calc(new GameHolder(canvas, () => Roll.apply()))

    Seq("keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave").foreach{s =>

      js.globals.window.document.body.addEventListener(s, (e: Event) => ribbonGame().event(e))
    }
    js.globals.setInterval(() => ribbonGame().update(), 10)
  }
}
