package roll
package gameplay
import acyclic.file
import scala.scalajs.js
import roll.cp.Cp
import org.scalajs.dom
import org.scalajs.dom.extensions._
import roll.cp.Implicits._
import roll.cp
import org.scalajs.dom


trait Drawable{
  def draw(ctx: dom.CanvasRenderingContext2D): Unit
}
object Drawable{
  case class Circle(radius: Double) extends Drawable{
    def draw(ctx: dom.CanvasRenderingContext2D) = {
      ctx.fillCircle(0, 0, radius)
      ctx.strokeCircle(0, 0, radius)
      ctx.strokePathOpen((0, radius/1.5), (0, radius))
    }

  }
  case class Polygon(points: Seq[(js.Number, js.Number)]) extends Drawable{
    def draw(ctx: dom.CanvasRenderingContext2D) = {
      ctx.fillPath(points: _*)
      ctx.strokePath(points: _*)
    }
  }
}

case class Form(body: cp.Body,
                shapes: Seq[cp.Shape],
                drawable: Drawable,
                color: Color){
  lazy val strokeStyle = color + Color(-64, -64, -64)
  lazy val fillStyle = color + Color(64, 64, 64)
}
class JointForm(val joint: cp.PivotJoint,
            val color: Color){
  lazy val strokeStyle = color + Color(-64, -64, -64)
  lazy val fillStyle = color + Color(64, 64, 64)
}

object Layers{
  val Common = 1
  val Static = Common << 1
  val Strokes = Static << 1
  val Fields = Strokes << 1

  val FirstNonReserved = Fields << 1

  val All = ~0
  val DynamicRange = All & ~Static & ~Strokes & ~Common & ~Fields
}

object Form{

  def makeCircle(pos: cp.Vect,
                 radius: Double,
                 density: Double,
                 static: Boolean,
                 friction: Double,
                 elasticity: Double,
                 layers: Int = 0)
                (implicit space: cp.Space) = {

    val body =
      if (static) space.staticBody
      else {
        val mass = math.abs(density * math.Pi * radius * radius)
        assert(mass > 0, (density, radius))
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

    shape.layers =
      if (layers != 0) layers
      else Layers.Common | (if (static) Layers.Static else Layers.DynamicRange)

    (body, Seq(shape))
  }
  def makePolySegments(points: Seq[cp.Vect],
               density: Double,
               friction: Double,
               elasticity: Double,
               layers: Int = 0)
              (implicit space: cp.Space) = {

    val shapes = for(i <- 0 until points.length) yield {
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
      shape.layers =
        if (layers != 0) layers
        else Layers.Static | Layers.Common
      shape

    }

    (space.staticBody, shapes, points.map(p => (p.x, p.y)))
  }
  def flatten(pts: Seq[cp.Vect]) = pts.flatMap(p => Seq(p.x, p.y)).toArray
  def flatten2(pts: Seq[(Double, Double)]) = pts.flatMap(p => Seq(p._1, p._2)).toArray
  def makePoly(points: Seq[cp.Vect],
               density: Double,
               friction: Double,
               elasticity: Double,
               layers: Int = 0)
              (implicit space: cp.Space) = {

    val flatPointsAbs = flatten(points)

    val center: cp.Vect = Cp.centroidForPoly(flatPointsAbs)
    val flatPoints = flatten(points.map(_ - center))

    val area = Cp.areaForPoly(flatPoints)
    val mass = math.abs(density * area)

    assert(mass > 0, (density, area))
    val body = space.addBody(
      new cp.Body(mass, Cp.momentForPoly(mass, flatPoints, (0, 0)))
    )
    body.setPos(center)
    val shape = space.addShape(
      new cp.PolyShape(body, flatPoints, (0, 0))
    )

    shape.setFriction(friction)
    shape.setElasticity(elasticity)
    shape.layers =
      if (layers != 0) layers
      else Layers.DynamicRange | Layers.Common
    (body, Seq(shape), flatPoints.grouped(2).map(s => (s(0), s(1))).toSeq)

  }

  def processJoint(elem: Xml.Circle)(implicit space: cp.Space): Seq[JointForm] = {

    val static = elem.misc.stroke != ""

    val color = if (elem.misc.fill != "") elem.misc.fill else "#000000"
    val (friction, springConstant, speed) = splitJointConfig(color)

    val shapes = collection.mutable.Buffer.empty[cp.Shape]
    space.pointQuery((elem.x, elem.y), ~0, 0, {(s: cp.Shape) => shapes += s; ()})

    var existing = Layers.Common | Layers.Static | Layers.Strokes
    var current = Layers.FirstNonReserved

    shapes.foreach{s =>
      val n = s.layers.toInt
      if (n != (Layers.Common | Layers.DynamicRange)) existing |= n
    }

    val baseBody = if (static || shapes.length == 0) space.staticBody else shapes(0).getBody()
    shapes.map{ s =>
      val joint = new cp.PivotJoint(
        s.getBody(),
        baseBody,
        (elem.x, elem.y),
        js.Dynamic.global.undefined.cast[cp.Vect]
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
      if (s.layers == (Layers.Common | Layers.DynamicRange) && !static){
        while((current & existing) != 0) {
          current <<= 1
        }
        s.setLayers(current)
        existing |= current
      }
      space.addConstraint(joint)
      new JointForm(joint, Color(color))
    }
  }

  /**
   * 0 =>   1/8
   * 0.5 => 1/2
   * 1 =>   4
   **/
  def splitFill(s: String): (Double, Double, Double) = {
    val parts = s.drop(1).grouped(2).toSeq

    val Seq(friction, density, elasticity) =
      parts.map(p => Integer.parseInt(p, 16) / 255.0)
           .map(p => math.pow(2, 6 * p) / 16)
           .toSeq

    (friction, density, elasticity)
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

  def processElement(elem: Xml,
                     static: Boolean,
                     layers: Int = 0)
                    (implicit space: cp.Space): Seq[Form] = {
//    dom.console.log("processElement", elem)
    elem match{
      case Xml.Polygon(pts, misc) =>
        val (friction, density, elasticity) = splitFill(misc.fill)
        val (body, shapes, flatPoints) =
          if (static) makePolySegments(pts.map(x => x: cp.Vect), density, friction, elasticity, layers)
          else makePoly(pts.map(x => x: cp.Vect), density, friction, elasticity, layers)
        Seq(new Form(
          body,
          shapes,
          Drawable.Polygon(flatPoints),
          Color(misc.fill)
        ))

      case Xml.Circle(x, y, r, misc) =>

        val (friction, density, elasticity)  = splitFill(misc.fill)
        val (body, shape) = Form.makeCircle(
          (x, y), r, density, static, friction, elasticity, layers
        )
        Seq(new Form(
          body,
          shape,
          Drawable.Circle(r),
          Color(misc.fill)
        ))

      case Xml.Group(children, misc) =>
        children.flatMap(processElement(_, static, layers))

      case _ =>
        println(" Unknown!")
        println(elem.getClass)
        ???
    }
  }
}
