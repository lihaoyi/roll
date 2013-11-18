import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import scala.js.workbench.refreshBrowsers
object Build extends sbt.Build {

  lazy val root = project.in(file("."))
    .settings(scalaJSSettings: _*)
    .settings(scala.js.resource.buildSettingsX: _*)
    .settings(scala.js.workbench.buildSettingsX: _*)
    .settings(
      name := "games",
      (managedSources in packageExportedProductsJS in Compile) := (managedSources in packageExportedProductsJS in Compile).value.filter(_.name.startsWith("00")),

      packageJS in Compile := {
        (packageJS in Compile).value ++ scala.js.resource.bundleJS.value :+ scala.js.workbench.generateClient.value
      },

      refreshBrowsers <<= refreshBrowsers.triggeredBy(packageJS in Compile)
    ).dependsOn(dom, resource, workbench)

  lazy val dom = RootProject(file("../scala-js-dom"))
  lazy val resource = RootProject(file("../scala-js-resource/runtime"))
  lazy val workbench = RootProject(file("../scala-js-workbench"))

}