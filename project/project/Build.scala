import sbt._
import Keys._


object Build extends sbt.Build {
  import sbt._

  override lazy val projects = Seq(root)
  lazy val root =
    Project("plugins", file("."))
      .dependsOn(uri("../../scala-js-resource/plugin"))
      .dependsOn(uri("../../WebSockets"))
      .settings(
        addSbtPlugin("org.scala-lang.modules.scalajs" % "scalajs-sbt-plugin" % "0.1-SNAPSHOT"),
        resolvers += "spray repo" at "http://repo.spray.io",

        resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
        libraryDependencies ++= Seq(
          "io.spray" % "spray-can" % "1.2-RC3",
          "com.typesafe.akka"   %%  "akka-actor"    % "2.2.3",
          "commons-codec" % "commons-codec" % "1.8",
          "com.typesafe.play" %% "play-json" % "2.2.0-RC1"
        )
      )
}