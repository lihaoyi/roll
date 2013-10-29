import sbt._
import Keys._
import ch.epfl.lamp.sbtscalajs.ScalaJSPlugin._
import ScalaJSKeys._

object Build extends sbt.Build {

  lazy val root = project.in(file(".")).settings(
    scalaJSSettings: _*
  ).settings(
    scala.js.resource.Plugin.resourceSettings:_*
  ).settings(
    name := "games",
    unmanagedSources in (Compile, ScalaJSKeys.packageJS) += baseDirectory.value / "js" / "startup.js"
  ).dependsOn(dom, resource)
  lazy val dom = RootProject(file("../scala-js-dom"))
  lazy val resource = RootProject(file("../scala-js-resource/runtime"))

}