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



class GameHolder(canvas: dom.HTMLCanvasElement){

  def run(inputs: Channel[Level.Input]) = task*async{
    class LevelData(val file: String, var completed: Boolean)
    val levels = List(
      "Demo.svg",
      "Steps.svg",
      "Ell.svg",
      "Assault.svg",
      "Descent.svg",
      "Bounce.svg",
      "Climb.svg",
      "BarrelWalk.svg"
    ).map(new LevelData(_, false))

    var selectedIndex = 0
    var running = false

    val game: Calc[Channel[Level.Input]] = Calc {
      val filteredChannel = new Channel.PubSub[Level.Input]
      gameplay.Level.run(level.file, filteredChannel).foreach{
        case Level.Result.Exit =>
          running = false
          game.recalc()
        case Level.Result.Next =>
          selectedIndex += 1
          game.recalc()
        case Level.Result.Reset => game.recalc()
      }
      filteredChannel
    }
    def level = levels(selectedIndex)

    val buffer = dom.document.createElement("canvas").asInstanceOf[dom.HTMLCanvasElement]
    val bufferCtx = buffer.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]


    def draw(ctx: dom.CanvasRenderingContext2D, viewPort: cp.Vect) = {
      ctx.drawImage(buffer, 0, 0)
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
          level.file + (if(level.completed) "✓" else ""),
          viewPort.x / 10,
          viewPort.y * 0.1 + i * rowHeight
        )
      }
    }

    println("GameHolde.run")
    while(true){
      val in = await(inputs())
      if (buffer.width != in.screenSize.x.toInt) buffer.width = in.screenSize.x.toInt
      if (buffer.height != in.screenSize.y.toInt) buffer.height = in.screenSize.y.toInt

      if (running) {
        println("running")
        game().update(in)
      } else {
        println("not running")
        game().update(Level.Input(Set(), Set(), Seq(), in.screenSize, bufferCtx))
        draw(in.painter, in.screenSize)
        if (in.keyPresses(KeyCode.enter)){
          running = true
        }
        if (in.keyPresses(KeyCode.down)) {
          selectedIndex = (selectedIndex + 1) % levels.length
          game.recalc()
        }
        if (in.keyPresses(KeyCode.up)) {
          selectedIndex = (selectedIndex - 1 + levels.length) % levels.length
          game.recalc()
        }
      }
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

    val gameHolder = new GameHolder(canvas)
    val touches = mutable.Buffer.empty[Touch]


    val interestedEvents = Seq(
      "keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave"
    )
    val keys = mutable.Set.empty[Int]
    val keyPresses = mutable.Set.empty[Int]
    val inputs = new Channel.PubSub[Level.Input]

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
        inputs.update(Level.Input(
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

    gameHolder.run(inputs)
  }
}
