package scala.js
import scalajs._
import acyclic.file
package object bundle {


  def apply(path: String) = {
    val current = js.Dynamic
                    .global
                    .ScalaJSBundle
                    .asInstanceOf[js.Dictionary[String]]
                    .apply(path)
                    .asInstanceOf[String]

    new Resource(current)
  }
  def create(value: String) = new Resource(value)

  class Resource(val base64: String){
    lazy val string = js.Dynamic.global.atob(base64).asInstanceOf[js.String]
  }

  println("scala-js-resource initialized")
}
