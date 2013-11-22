package example

import scala.scalajs.js
import example.cp.Cp
import scala.scalajs.js.{SVGRectElement, SVGElement, Element}
import scala.scalajs.extensions._
import example.cp.Implicits._

object Forms{

  def makeCircle(pos: cp.Vect,
                 radius: Double,
                 density: Double,
                 static: Boolean,
                 friction: Double,
                 elasticity: Double)
                (implicit space: cp.Space) = {

    val body =
      if (static) space.staticBody
      else {
        val mass = density * math.Pi * radius * radius + 0.000000001
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

  def makePoly(points: Seq[cp.Vect],
               density: Double,
               static: Boolean,
               friction: Double,
               elasticity: Double)
              (implicit space: cp.Space) = {
    val flatPoints: js.Array[js.Number] = points.flatMap(p => Seq[js.Number](p.x, p.y)).toArray[js.Number]
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

  def processJoint(elem: Element)(implicit space: cp.Space): Seq[cp.PivotJoint] = {
    val Seq(x, y, r) = Seq("cx", "cy", "r").map{c =>
      elem.getAttribute(c).toString.toDouble
    }
    val static = elem.hasAttribute("stroke")

    val color = Option[String](elem.getAttribute("fill"))
    val (friction, springConstant, speed) = splitJointConfig(
      color.getOrElse("#000000")
    )

    val shapes = collection.mutable.Buffer.empty[cp.Shape]
    space.pointQuery((x, y), ~0, 0, {(s: cp.Shape) => shapes += s; ()})

    var existing = 0
    var current = 1

    shapes.foreach{s =>
      val n = s.layers.toInt
      if (n != ~0) existing |= n
    }

    val baseBody = if (static) space.staticBody else shapes(0).getBody()
    shapes.map{ s =>
      val joint = new cp.PivotJoint(
        s.getBody(),
        baseBody,
        (x, y),
        js.Dynamic.global.undefined.asInstanceOf[cp.Vect]
      )
      val effectiveI = 1.0/ (1.0 / baseBody.i + 1.0 / s.getBody().i)
      if (springConstant != 0 || friction != 0){


        val springJoint = new cp.DampedRotarySpring(
          s.getBody(),
          baseBody,
          restAngle = baseBody.a - s.getBody().a,
          stiffness = effectiveI * springConstant * 1.5,
          damping = effectiveI * friction
        )

        space.addConstraint(springJoint)
      }
      if (speed != 0){
        val motorJoint = new cp.SimpleMotor(s.getBody(), baseBody, speed)
        motorJoint.maxForce = math.abs(speed) * effectiveI * 10
        space.addConstraint(motorJoint)
      }
      if (s.layers.toInt == ~0 && !static){

        while((current & existing) != 0) current *= 2
        s.setLayers(current)

        existing |= current
      }
      space.addConstraint(joint)
      joint
    }
  }

  /**
   * 0 =>   1/8
   * 0.5 => 1/2
   * 1 =>   4
   **/
  def splitFill(s: String): (Double, Double, Double) = {
    val parts = s.drop(1).grouped(2).toSeq

    val Seq(elasticity, density, friction) =
      parts.map(p => Integer.parseInt(p, 16) / 255.0)
           .map(p => math.pow(2, 6 * p) / 16)
           .toSeq

    (elasticity, density, friction)
  }
  def splitJointConfig(s: String): (Double, Double, Double) = {
    val parts = s.drop(1).grouped(2).toSeq

    val Seq(friction, springConstant, speed) =
      parts.map(p => Integer.parseInt(p, 16)).toSeq


    val lowBit = 1
    val res = (
      math.tan(friction * math.Pi / 2 / 255.1),
      math.tan(springConstant * math.Pi / 2 / 255.1),
      (if ((lowBit & speed) != 0) -1 else 1) * (~lowBit & speed) * 10.0 / 254.0
    )
    res
  }
  def processElement(elem: Element,
                     static: Boolean)
                    (implicit space: cp.Space): cp.Body = {

    elem match{
      case elem: js.SVGRectElement =>
        val svg = js.JsGlobals.document.createElementNS("http://www.w3.org/2000/svg", "svg").asInstanceOf[js.SVGSVGElement]
        val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map{c =>
          elem.getAttribute(c).toString.toDouble
        }

        var svgPt = svg.createSVGPoint()

        val transforms = elem.transform.baseVal
        val pos = new cp.Vect(x, y)
        val dims = new cp.Vect(w, h)

        val points = Seq(pos, pos + new cp.Vect(0, dims.y), pos + dims, pos + new cp.Vect(dims.x, 0))

        val transformedPoints =
          points.map{p =>
            svgPt.x = p.x
            svgPt.y = p.y
            for (transform <- transforms){
              svgPt = svgPt.matrixTransform(transform.matrix)
            }
            new cp.Vect(svgPt.x, svgPt.y)
          }

        val (elasticity, density, friction) = splitFill(elem.getAttribute("fill"))
        makePoly(transformedPoints, density, static, friction, elasticity)

      case _: js.SVGPolylineElement =>
        val points =
          elem
            .getAttribute("points")
            .toString
            .split("\\s+")
            .toSeq
            .map(s => s.split(","))
            .map(p => new cp.Vect(p(0).toDouble, p(1).toDouble))
        val (elasticity, density, friction) = splitFill(elem.getAttribute("fill"))

        Forms.makePoly(points, density, static, friction, elasticity)

      case elem: js.SVGPolygonElement => null

      case elem: js.SVGCircleElement =>
        val Seq(x, y, r) = Seq("cx", "cy", "r").map{c =>
          elem.getAttribute(c).toString.toDouble
        }
        val (elasticity, density, friction) = splitFill(elem.getAttribute("fill"))
        Forms.makeCircle((x, y), r, density, static, friction, elasticity)

      case _ =>
        println("Unknown!")
        js.JsGlobals.console.log(elem)
        js.Dynamic.global.elem = elem
        ???
    }
  }
}
