package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js

class Camera(ctx: CanvasRenderingContext2D, targetPos: => cp.Vect, canvasDims: => cp.Vect, var scale: cp.Vect){
  var pos = new cp.Vect(targetPos.x, targetPos.y)
  def update(dt: Double, keys: Set[Int]) = {
    if (keys(KeyCode.pageUp)) scale *= 1.01
    if (keys(KeyCode.pageDown)) scale /= 1.01

    if (pos != targetPos){
      pos = targetPos * 0.03 + pos * 0.97
    }
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
    case e: PointerEvent if e.`type` == "pointerout" =>
    case e: PointerEvent if e.`type` == "pointerleave" =>
    case _ => println("Unknown event " + e.`type`)
  }

  var active = false

  var now = Calc(Date.now() / 1000)

  def update() = {
    canvas.width = JsGlobals.innerWidth
    canvas.height = JsGlobals.innerHeight
    val oldNow = now()
    now.recalc()
    camera.update(now() - oldNow, keys.toSet)
    game.update(keys.toSet, lines)
    lines = Nil

    camera.transform{ ctx =>
      game.draw(ctx)
    }
  }
}

abstract class Game{
  var result: Option[String] = None
  def update(keys: Set[Int], lines: Seq[(cp.Vect, cp.Vect)]): Unit
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


    Seq("keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave").foreach(s =>
      canvas.addEventListener(s, (e: Event) => ribbonGame().event(e))
    )
    JsGlobals.setInterval(() => ribbonGame().update(), 10)
  }
}
