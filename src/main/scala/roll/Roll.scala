package roll
import acyclic.file
import scala.collection.mutable
import scala.scalajs.js._
import scala.scalajs.js.Any._
import cp.Implicits._
import org.scalajs.dom.extensions._
import org.scalajs.dom
import roll.gameplay.Level
import scala.scalajs.js.annotation.JSExport
import scala.async.Async._
import scala.concurrent.{Promise, Future}
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import roll.gameplay.Level.Input


class GameHolder(canvas: dom.HTMLCanvasElement){
  class LevelData(val file: String,
                  var completed: Boolean,
                  var inputs: Seq[Level.Input])
  val levels = List(
    "levels/Demo.svg",
    "levels/Steps.svg",
    "levels/Ell.svg",
    "levels/Assault.svg",
    "levels/OverUnder.svg",
    "levels/Vortex.svg",
    "levels/Collector.svg",
    "levels/KineticDream.svg",
    "levels/Descent.svg",
    "levels/Bounce.svg",
    "levels/Climb.svg",
    "levels/BarrelWalk.svg"
  ).map(new LevelData(_, false, Seq()))

  var selectedIndex = 0
  def next() = {
    selectedIndex = (selectedIndex + 1) % levels.length
    game.recalc()
  }
  def prev() = {
    selectedIndex = (selectedIndex - 1 + levels.length) % levels.length
    game.recalc()
  }
  def level = levels(selectedIndex)
  var running = false
  var storedInputs: List[Level.Input] = Nil
  val game = Calc {
    new gameplay.Level(level.file, new cp.Vect(canvas.width, canvas.height))
  }


  def draw(ctx: dom.CanvasRenderingContext2D, viewPort: cp.Vect) = {
    ctx.fillStyle = "rgba(0, 0, 0, 0.25)"
    ctx.fillRect(0, 0, viewPort.x, viewPort.y)
    val rowHeight = viewPort.y * 0.8 / levels.length
    for((level, i) <- levels.zipWithIndex){
      ctx.fillStyle =
        if (i == selectedIndex) "yellow"
        else if(level.completed) "SpringGreen"
        else "white"

      ctx.font = rowHeight.toInt + "px Lucida Grande"
      ctx.textBaseline = "top"
      ctx.fillText(
        level.file.drop("levels/".length).dropRight(".svg".length) + (if(level.completed) "✓" else ""),
        viewPort.x / 10,
        viewPort.y * 0.1 + i * rowHeight
      )
    }
  }


  def update(in: Input) = {

    if (running) {
      game().update(in) match {
        case Level.Result.Exit =>
          game.recalc()
          running = false
        case Level.Result.Next => next()
        case Level.Result.Reset => game.recalc()
        case _ =>
      }
      storedInputs = in :: storedInputs
    } else {
      if (level.inputs == Nil) {
        game().update(Level.Input(Set(), Set(), Seq(), in.screenSize, in.painter))
      } else {
        dom.console.log(level.inputs.length)
        game().update(level.inputs.head.copy(painter = in.painter))
        level.inputs = level.inputs.tail
      }
      draw(in.painter, in.screenSize)
      if (in.keyPresses(KeyCode.enter)){
        running = true
      }
      if (in.keyPresses(KeyCode.down)) next()
      if (in.keyPresses(KeyCode.up)) prev()
    }
  }

}

object Roll extends scalajs.js.JSApp{
  /**
   * Main method. This is also responsible for taking all the scary Javascript
   * idioms (events, callbacks, blargh) and packaging it up into nice Channel
   * for the rest of the code to consume.
   */
  def main(): Unit = {
    println("main")

    val canvas =
      dom.document
        .getElementById("canvas")
        .cast[dom.HTMLCanvasElement]

    val  gameHolder = new GameHolder(canvas)
    val touches = mutable.Buffer.empty[Touch]


    val interestedEvents = Seq(
      "keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave"
    )
    val keys = mutable.Set.empty[Int]
    val keyPresses = mutable.Set.empty[Int]

    interestedEvents.foreach{s =>
      dom.document.body.addEventListener(s, { (e: dom.Event) =>

        (e, e.`type`.toString) match {
          case (e: dom.KeyboardEvent, "keydown") =>
            keys.add(e.keyCode)
            keyPresses.add(e.keyCode)
          case (e: dom.KeyboardEvent, "keyup") => keys.remove(e.keyCode)
          case (e: PointerEvent, "pointerdown") => touches += Touch.Down((e.clientX, e.clientY))
          case (e: PointerEvent, "pointermove") => touches += Touch.Move((e.clientX, e.clientY))
          case (e: PointerEvent, "pointerup" | "pointerout" | "pointerleave") => touches += Touch.Up((e.clientX, e.clientY))
          case _ => println("Unknown event " + e.`type`)
        }
      })
    }
    val painter = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    dom.setInterval(
      () => {
        if (canvas.width != dom.innerWidth) canvas.width = dom.innerWidth
        if (canvas.height != dom.innerHeight) canvas.height = dom.innerHeight
        gameHolder.update(Level.Input(
          keys.toList.toSet,
          keyPresses.toList.toSet,
          touches.toList,
          (canvas.width, canvas.height),
          painter
        ))
        touches.clear()
        keyPresses.clear()
      },
      15
    )
  }
}
