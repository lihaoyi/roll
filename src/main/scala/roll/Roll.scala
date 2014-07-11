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

  def run(inputs: Channel[Level.Input],
          frames: Channel[dom.CanvasRenderingContext2D]) = async{
    var levels = List(
      "Demo.svg",
      "Descent.svg",
      "Bounce.svg",
      "Climb.svg",
      "BarrelWalk.svg"
    )

    println("GameHolde.run")
    while(true){
      println("GameHolde.run loop")
      if (canvas.width != dom.innerWidth) canvas.width = dom.innerWidth
      if (canvas.height != dom.innerHeight) canvas.height = dom.innerHeight

      val result = gameplay.Level.run(levels.head, inputs)
      await(result) match {
        case Level.Result.Next => levels = levels.tail
        case Level.Result.Reset => // do nothing
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

    val ribbonGame = new GameHolder(canvas)
    val touches = mutable.Buffer.empty[Touch]
    val keys = mutable.Set.empty[Int]

    val interestedEvents = Seq(
      "keyup", "keydown", "pointerdown", "pointermove", "pointerup", "pointerleave"
    )

    val inputs = new Channel.PubSub[Level.Input]
    
    interestedEvents.foreach{s =>
      dom.document.body.addEventListener(s, { (e: dom.Event) =>

        (e, e.`type`.toString) match {
          case (e: dom.KeyboardEvent, "keydown") => keys.add(e.keyCode)
          case (e: dom.KeyboardEvent, "keyup") => keys.remove(e.keyCode)
          case (e: PointerEvent, "pointerdown") =>
            println("pointerdown Event")
            touches += Touch.Down((e.clientX, e.clientY))
          case (e: PointerEvent, "pointermove") => touches += Touch.Move((e.clientX, e.clientY))
          case (e: PointerEvent, "pointerup" | "pointerout" | "pointerleave") => touches += Touch.Up((e.clientX, e.clientY))
          case _ => println("Unknown event " + e.`type`)
        }
      })
    }
    val painter = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    dom.setInterval(
      () => {
        inputs.update(Level.Input(
          keys.toList.toSet,
          touches.toList,
          (canvas.width, canvas.height),
          painter
        ))
        keys.clear()
        touches.clear()
      },
      15
    )

    ribbonGame.run(inputs)
  }
}
