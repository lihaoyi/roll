package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import org.scalajs.dom.extensions._
import org.scalajs.dom
import example.roll.{Roll, Camera}


class GameHolder(canvas: dom.HTMLCanvasElement, gameMaker: (() => cp.Vect) => Game){

  def bounds = new cp.Vect(canvas.width, canvas.height)

  private[this] val keys = mutable.Set.empty[Int]

  var game: Game = gameMaker(() => bounds)

  val scale = math.min(dom.innerWidth / game.widest.x, dom.innerHeight / game.widest.y)

  var camera: Camera = new Camera.Pan(
    () => bounds,
    game.widest,
    checkpoints = List(
      (game.startCameraPos, 1),
      (game.widest / 2, scale)
    ),
    finalCamera = new Camera.Follow(
      game.cameraPos,
      game.widest,
      () => bounds,
      1
    )
  )

  var prev: cp.Vect = null
  var lines: List[(cp.Vect, cp.Vect)] = Nil

  def screenToWorld(p: cp.Vect) = ((p - bounds/2) / camera.scale) + camera.pos

  def event(e: dom.Event): Unit = (e, e.`type`.toString) match{
    case (e: dom.KeyboardEvent, "keydown") =>  keys.add(e.keyCode.toInt)
    case (e: dom.KeyboardEvent, "keyup") =>  keys.remove(e.keyCode.toInt)
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
  val ctx = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  def update() = {
    if (canvas.width != dom.innerWidth) canvas.width = dom.innerWidth
    if (canvas.height != dom.innerHeight) canvas.height = dom.innerHeight
    val oldNow = now()
    now.recalc()
    camera.update(now() - oldNow, keys.toSet)
    game.update(keys.toSet, lines, prev != null)

    lines = Nil


    ctx.fillStyle = "#82CAFF"
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
  def draw(ctx: dom.CanvasRenderingContext2D): Unit
  def drawStatic(ctx: dom.CanvasRenderingContext2D, w: Int, h: Int): Unit
  def cameraPos: cp.Vect
  def startCameraPos: cp.Vect
  def widest: cp.Vect
}

object ScalaJSExample {
  def main(): Unit = {
//    dom.console.log("Hello World")


    val canvas =
      dom.document
         .getElementById("screen")
         .asInstanceOf[dom.HTMLCanvasElement]

    val ribbonGame = Calc(new GameHolder(canvas, x => Roll(x)))

    Seq("keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave").foreach{s =>

      dom.document.body.addEventListener(s, (e: dom.Event) => ribbonGame().event(e))
    }
    dom.setInterval(() => ribbonGame().update(), 10)
  }
}
