import sbt._
import Keys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._
import com.lihaoyi.workbench.Plugin.{updateBrowsers, bootSnippet, workbenchSettings}
import org.apache.commons.codec.binary.Base64



object Build extends sbt.Build {

  object Bundle extends sbt.Plugin {
    val bundledDirectory = settingKey[File]("The folder where all to-be-bundled resources comes from")
    val bundleName = settingKey[String]("The final name of the resource bundle")
    val bundleJS = taskKey[Set[File]]("bundles your filesystem tree inside bundleDirectory into a single file")
    val buildSettingsX = Seq(
      watchSources := {
        watchSources.value ++ Path.allSubpaths(bundledDirectory.value).map(_._1).toSeq
      },
      bundledDirectory := (sourceDirectory in Compile).value / "bundled",
      bundleName := "bundled.js",
      bundleJS := {

        val cacheFiles = for {
          (file, path) <- Path.allSubpaths(bundledDirectory.value)
          if !file.isDirectory
        } yield {
          FileFunction.cached(
            cacheDirectory.value / "bundled" / path,
            FilesInfo.lastModified,
            FilesInfo.exists
          ){(inFiles: Set[File]) =>
            val data = Base64.encodeBase64String(IO.readBytes(inFiles.head)).replaceAll("\\s", "")
            val outFile = cacheDirectory.value / "bundled" / path / "base64data"
            IO.write(outFile, s""""$path": "$data" """)
            Set(outFile)
          }(Set(file))
        }

        FileFunction.cached(
          cacheDirectory.value / "totalBundle",
          FilesInfo.lastModified,
          FilesInfo.exists
        ){(inFiles: Set[File]) =>
          val bundle = crossTarget.value / bundleName.value
          val fileLines = inFiles.map(IO.read(_))
          IO.write(bundle, "\nScalaJSBundle = {\n" + fileLines.mkString(",\n") + "\n}" )
          Set(bundle)
        }(cacheFiles.toSet.flatten)
      }
    )
  }

  lazy val root = project.in(file("."))
    .settings(scalaJSSettings: _*)
    .settings(Bundle.buildSettingsX: _*)
    .settings(workbenchSettings: _*)
    .settings(
      name := "games",
      scalaVersion := "2.11.2",
      bootSnippet := "roll.Roll().main();",
      libraryDependencies ++= Seq(
        "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6",
        "org.scala-lang.modules" %% "scala-async" % "0.9.1",
        "com.lihaoyi" %% "acyclic" % "0.1.2" % "provided"
      ),
      autoCompilerPlugins := true,
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % "0.1.2"),
//      scalacOptions := Seq("-Xexperimental"),
      (resources in Compile) := (resources in Compile).value ++ Bundle.bundleJS.value,
      updateBrowsers <<= updateBrowsers.triggeredBy(fastOptJS in Compile)
    )

}
