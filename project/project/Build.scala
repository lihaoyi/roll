import sbt._
import Keys._


object Build extends sbt.Build {
  import sbt._

  override lazy val projects = Seq(root)
  lazy val root =
    Project("plugins", file("."))
      .dependsOn(uri("../../scala-js-resource/plugin"))
      .settings(
        addSbtPlugin("ch.epfl.lamp" % "scalajs-sbt-plugin" % "0.1-SNAPSHOT")
      )
}