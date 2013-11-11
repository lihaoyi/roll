package example

import scala.scalajs.js
import example.cp.Cp
import scala.scalajs.js.Element

import example.cp.Implicits._

object Forms{
  def makeCircle(pos: cp.Vect,
                 radius: Double,
                 density: Double,
                 static: Boolean,
                 friction: Double = 0.3,
                 elasticity: Double = 0.3)
                (implicit space: cp.Space) = {


    val body =
      if (static) space.staticBody
      else {
        println("MAKE CIRCLE DYNAMIC")
        val mass = density * math.Pi * radius * radius
        val moment = 0.5 * mass * radius * radius
        val newBody = space.addBody(
          new cp.Body(mass, moment)
        )
        newBody.setPos(pos)
        newBody
      }

    val shape = space.addShape(
      new cp.CircleShape(body, radius, (0, 0))
    )
    shape.setFriction(friction)
    shape.setElasticity(elasticity)
    body
  }
  def makeRect(pos: cp.Vect,
               dims: cp.Vect,
               density: Double,
               static: Boolean,
               friction: Double = 0.3,
               elasticity: Double = 0.3)
              (implicit space: cp.Space) = {
    makePoly(
      Seq(pos, pos + new cp.Vect(0, dims.y), pos + dims, pos + new cp.Vect(dims.x, 0)),
      density,
      static,
      friction,
      elasticity
    )
  }

  def makePoly(points: Seq[cp.Vect],
               density: Double,
               static: Boolean,
               friction: Double,
               elasticity: Double)
              (implicit space: cp.Space) = {
    val flatPoints: js.Array[js.Number] = points.flatMap(p => Seq(p.x: js.Number, p.y: js.Number)).toArray[js.Number]
    val center = Cp.centroidForPoly(flatPoints)

    Cp.recenterPoly(flatPoints)
    val area = Cp.areaForPoly(flatPoints)
    val mass = density * area

    if (static) {
      for(i <- 0 until points.length){
        val p1 = points(i)
        val p2 = points((i + 1) % points.length)
        val shape = new cp.SegmentShape(
          space.staticBody,
          (p1.x, p1.y),
          (p2.x, p2.y),
          0
        )
        space.addShape(shape)
        shape.setFriction(friction)
        shape.setElasticity(elasticity)
      }

      space.staticBody
    }else{
      val body = space.addBody(
        new cp.Body(mass, Cp.momentForPoly(mass, flatPoints, (0, 0)))
      )
      body.setPos(center)
      val shape = space.addShape(
        new cp.PolyShape(body, flatPoints, (0, 0))
      )

      shape.setFriction(friction)
      shape.setElasticity(elasticity)
      body
    }
  }
  def processElement(elem: Element,
                     density: Double,
                     friction: Double,
                     elasticity: Double,
                     static: Boolean)
                    (implicit space: cp.Space) = {

    elem.nodeName.toString match{
      case "rect" =>

        val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map{c =>
          elem.getAttribute(c).toString.toDouble
        }
        Forms.makeRect(
          pos = (x, y),
          dims = (w, h),
          density = density,
          static = static,
          elasticity = elasticity,
          friction = friction
        )
      case "polyline" =>
        val points =
          elem.getAttribute("points")
            .toString
            .split("\\s+")
            .toSeq
            .map(s => s.split(","))
            .map(p => new cp.Vect(p(0).toDouble, p(1).toDouble))


        Forms.makePoly(
          points,
          density,
          static,
          friction,
          elasticity
        )
      case "circle" =>
        val Seq(x, y, r) = Seq("cx", "cy", "r").map{c =>
          elem.getAttribute(c).toString.toDouble
        }
        println(x + "    " + y + "    " + r)
        Forms.makeCircle(
          (x, y),
          r,
          density,
          static,
          friction,
          elasticity
        )

      case _ =>
    }
  }
}
