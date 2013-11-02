package example

import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._

object Color{
  def rgb(r: Int, g: Int, b: Int) = s"rgb($r, $g, $b)"
  val White = rgb(255, 255, 255)
  val Red = rgb(255, 0, 0)
  val Green = rgb(0, 255, 0)
  val Blue = rgb(0, 0, 255)
  val Cyan = rgb(0, 255, 255)
  val Magenta = rgb(255, 0, 255)
  val Yellow = rgb(255, 255, 0)
  val Black = rgb(0, 0, 0)
  val all = Seq(
    White,
    Red,
    Green,
    Blue,
    Cyan,
    Magenta,
    Yellow,
    Black
  )
}


class GameHolder(canvasName: String, gameMaker: (() => Point, () => Unit) => Game){
  private[this] val canvas = JsGlobals.window.document.getElementById(canvasName).asInstanceOf[HTMLCanvasElement]
  private[this] def bounds = Point(canvas.width, canvas.height)
  private[this] val keys = mutable.Set.empty[Int]
  var game: Game = gameMaker(bounds _, () => resetGame())

  canvas.onkeydown = {(e: KeyboardEvent) =>
    keys.add(e.keyCode.toInt)
    if (Seq(32, 37, 38, 39, 40).contains(e.keyCode.toInt)) e.preventDefault()
    message = None
  }
  canvas.onkeyup = {(e: KeyboardEvent) =>
    keys.remove(e.keyCode.toInt)
    if (Seq(32, 37, 38, 39, 40).contains(e.keyCode.toInt)) e.preventDefault()
  }

  canvas.onfocus = {(e: FocusEvent) =>
    active = true
  }
  canvas.onblur = {(e: FocusEvent) =>
    active = false
  }

  private[this] val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  var active = false
  var firstFrame = false
  def update() = {
    if (!firstFrame){
      game.draw(ctx)
      firstFrame = true
    }
    canvas.width = JsGlobals.innerWidth
    canvas.height = JsGlobals.innerHeight
    if (active && message.isEmpty) {
      game.draw(ctx)
      game.update(keys.toSet)
    }else if (message.isDefined){
      ctx.fillStyle = Color.Black
      ctx.fillRect(0, 0, bounds.x, bounds.y)
      ctx.fillStyle = Color.White
      ctx.font = "20pt Arial"
      ctx.textAlign = "center"
      ctx.fillText(message.get, bounds.x/2, bounds.y/2)
      ctx.font = "14pt Arial"
      ctx.fillText("Press any key to continue", bounds.x/2, bounds.y/2 + 30)
    }
  }

  var message: Option[String] = None
  def resetGame(): Unit = {
    message = game.result
    println("MESSAGE " + message)
    game = gameMaker(bounds _, () => resetGame())
  }
  ctx.font = "12pt Arial"
  ctx.textAlign = "center"
}
abstract class Game{
  var result: Option[String] = None
  def update(keys: Set[Int]): Unit

  def draw(ctx: CanvasRenderingContext2D): Unit
}

object ScalaJSExample {
  def main(): Unit = {
    println("Main")
    val ribbonGame = new GameHolder("screen", Tetris)
    val games = Seq(ribbonGame)
    JsGlobals.setInterval(() => games.foreach(_.update()), 15)
  }

  def loadFile(path: String) = {
    new XMLHttpRequest().open("GET", path)
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
