import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import akka.io
import akka.io.Tcp
import akka.io.Tcp.Register
import akka.util.ByteString
import com.typesafe.config.{ConfigFactory, Config}
import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import spray.can.Http
import spray.can.server.websockets.model.OpCode.Text
import spray.can.server.websockets.model.{OpCode, Frame}
import spray.can.server.websockets.Sockets
import spray.http.HttpHeaders.Connection
import spray.http.{StatusCodes, HttpResponse, HttpRequest}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import collection.mutable
object Build extends sbt.Build {
  implicit val system = ActorSystem(
    "SystemLol",
    config = ConfigFactory.load(ActorSystem.getClass.getClassLoader),
    classLoader = ActorSystem.getClass.getClassLoader
  )
  class SocketServer extends Actor{
    val sockets: mutable.Set[ActorRef] = mutable.Set.empty
    def receive = {
      case x: Tcp.Connected => sender ! Register(self) // normal Http server init

      case req: HttpRequest =>
        // Upgrade the connection to websockets if you think the incoming
        // request looks good
      if (req.headers.contains(Connection("Upgrade"))){
          sender ! Sockets.UpgradeServer(Sockets.acceptAllFunction(req), self)
        }else{
          sender ! HttpResponse(
            StatusCodes.OK,
            entity="i am a cow"
          )
        }

      case Sockets.Upgraded =>
        sockets.add(sender)
        streams.value.log(sockets.size + "Browsers open")

      case f @ Frame(fin, rsv, Text, maskingKey, data) =>
        sockets.foreach(_ ! f.copy(maskingKey=None))

      case _: Tcp.ConnectionClosed =>
        sockets.remove(sender)
        streams.value.log(sockets.size + "Browsers open")

      case x => println("WTF IS THIS " + x)
    }
  }

  val server = system.actorOf(Props(new SocketServer))

  io.IO(Sockets) ! Http.Bind(
    server,
    "localhost",
    12345
  )
  def send(x: JsArray) = {
    server ! Frame(
      opcode = OpCode.Text,
      data = ByteString(x.toString())
    )
  }
  val clientLogger = FullLogger{
    new Logger {
      def log(level: Level.Value, message: => String): Unit =
        if(level >= Level.Info) send(Json.arr("print", level.toString(), message))

      def success(message: => String): Unit =
        send(Json.arr("print", message))

      def trace(t: => Throwable): Unit =
        send(Json.arr("print", t.toString))

    }
  }

  lazy val root = project.in(file("."))
    .settings(scalaJSSettings: _*)
    .settings(scala.scalajs.js.resource.Plugin.resourceSettings:_*)
    .settings(
      extraLoggers := {
        val currentFunction = extraLoggers.value
        (key: ScopedKey[_]) => clientLogger +: currentFunction(key)
      },
      name := "games",
      (managedSources in packageExportedProductsJS in Compile) := (managedSources in packageExportedProductsJS in Compile).value.filter(_.name.startsWith("00")),
      packageJS := {
        streams.value.log("Reloading Pages...")
        send(Json.arr("reload"))
        (packageJS in Compile).value
      }
    ).dependsOn(dom, resource)

  lazy val dom = RootProject(file("../scala-js-dom"))
  lazy val resource = RootProject(file("../scala-js-resource/runtime"))

}