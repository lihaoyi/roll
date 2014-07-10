package roll

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

sealed trait Touch 
object Touch{
  case class Down(p: cp.Vect) extends Touch
  case class Move(p: cp.Vect) extends Touch
  case class Up(p: cp.Vect) extends Touch
}

trait Result
object Result{
  case object Next extends Result
  case object Reset extends Result
}

class GameHolder(canvas: dom.HTMLCanvasElement){

  def bounds = new cp.Vect(canvas.width, canvas.height)

  var levels = List(
    "Demo.svg",
    "Descent.svg",
    "Bounce.svg",
    "Climb.svg",
    "BarrelWalk.svg"
  )

  updateCanvas()

  def run(inputs: Channel[Input]) = async{
    println("GameHolde.run")
    while(true){
      println("GameHolde.run loop")
      val result = gameplay.Level.run(levels.head, inputs)
      await(result) match {
        case Result.Next => levels = levels.tail
        case Result.Reset => // do nothing
      }
    }
  }



  var active = false

  val ctx = canvas.getContext("2d").cast[dom.CanvasRenderingContext2D]

  def updateCanvas() = {
    if (canvas.width != dom.innerWidth) canvas.width = dom.innerWidth
    if (canvas.height != dom.innerHeight) canvas.height = dom.innerHeight
  }
  def update() = {
    updateCanvas()

//    game().update(keys.toSet, x)
//    game().draw(ctx)
  }
}

object Roll extends scalajs.js.JSApp{

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

    val inputs = new Channel.PubSub[Input]
    
    interestedEvents.foreach{s =>
      dom.document.body.addEventListener(s, { (e: dom.Event) =>
        (e, e.`type`.toString) match {
          case (e: dom.KeyboardEvent, "keydown") => keys.add(e.keyCode)
          case (e: dom.KeyboardEvent, "keyup") => keys.remove(e.keyCode)
          case (e: PointerEvent, "pointerdown") => touches += Touch.Down((e.clientX, e.clientY))
          case (e: PointerEvent, "pointermove") => touches += Touch.Move((e.clientX, e.clientY))
          case (e: PointerEvent, "pointerup" | "pointerout" | "pointerleave") => touches += Touch.Up((e.clientX, e.clientY))
          case _ => println("Unknown event " + e.`type`)
        }
      })
    }

    dom.setInterval(
      () => {
        inputs.update(Input(
          keys.toSet,
          touches.toSeq,
          (canvas.width, canvas.height),
          canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
        ))
        keys.clear()
        touches.clear()
      },
      15
    )

    ribbonGame.run(inputs)
  }
}
case class Input(keys: Set[Int],
                 touches: Seq[Touch],
                 screenSize: cp.Vect,
                 painter: dom.CanvasRenderingContext2D)
