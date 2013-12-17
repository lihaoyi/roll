import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import scala.js.workbench.{updateBrowsers, bootSnippet}
object Build extends sbt.Build {

  lazy val root = project.in(file("."))
    .settings(scalaJSSettings: _*)
    .settings(scala.js.bundle.buildSettingsX: _*)
    .settings(scala.js.workbench.buildSettingsX: _*)
    .settings(
      name := "games",
      bootSnippet := "ScalaJS.modules.example_ScalaJSExample().main();",
      (managedSources in packageExportedProductsJS in Compile) := (managedSources in packageExportedProductsJS in Compile).value.filter(_.name.startsWith("00")),

      packageJS in Compile := {
        (packageJS in Compile).value ++ scala.js.bundle.bundleJS.value :+ scala.js.workbench.generateClient.value
      },

      updateBrowsers <<= updateBrowsers.triggeredBy(packageJS in Compile)
    ).dependsOn(dom, resource, workbench)

  lazy val dom = RootProject(file("../scala-js-dom"))
  lazy val resource = RootProject(file("../scala-js-resource/runtime"))
  lazy val workbench = RootProject(file("../scala-js-workbench"))

}