package roll.cp
import acyclic.file
import scala.scalajs.js.annotation.JSName
import scala.scalajs.js


@JSName("cp.Vect")
class Vect(var x: Double, var y: Double) extends js.Object
@JSName("cp.BB")
class BB(var l: Double, var b: Double, var r: Double, var t: Double) extends js.Object

@JSName("cp.Shape")
class Shape(body: Body) extends js.Object{
  def setElasticity(e: Double): Unit = ???
  def setFriction(u: Double): Unit = ???
  def setLayers(layers: Double): Unit = ???
  def setSensor(sensor: js.Any): Unit = ???
  def setCollisionType(collisionType: js.Any): Unit = ???
  def getBody(): Body = ???
  def active(): js.Boolean = ???
  def setBody(b: Body) = ???
  def cacheBB(): js.Any = ???
  def getBB(): BB = ???
  def update(pos: Vect, rot: Double): js.Any = ???
  def pointQuery(p: Vect): js.UndefOr[NearestPointQueryInfo] = ???
  var layers: Int = ???
  var group: Int = ???
}

@JSName("cp.PointQueryExtendedInfo")
class PointQueryExtendedInfo(var shape: Shape) extends js.Object
@JSName("cp.NearestPointQueryInfo")
class NearestPointQueryInfo(var shape: Shape, p: Vect, val d: Double) extends js.Object
@JSName("cp.SegmentQueryInfo")
class SegmentQueryInfo(var shape: Shape, var t: Double, var n: Vect) extends js.Object

@JSName("cp.CircleShape")
class CircleShape(body: Body, radius: Double, var offset: Vect) extends Shape(body){
  var r: Double = ???
}
@JSName("cp.SegmentShape")
class SegmentShape(body: Body, var a: Vect, var b: Vect, var r: Double) extends Shape(body)
@JSName("cp.PolyShape")
class PolyShape(body: Body, var verts: js.Array[Double], var offset: Vect) extends Shape(body)

@JSName("cp.BoxShape")
object BoxShape extends js.Object{
  def apply(body: Body, width: Double, height: Double): PolyShape = ???
}
@JSName("cp.Body")
class Body(val m: Double, val i: Double) extends js.Object{
  var a: Double = ???
  var f:  Vect= ???
  var t: Double = ???
  var w: Double = ???
  var shapeList: js.Array[Shape] = ???

  def getPos(): Vect = ???
  def getVel(): Vect = ???
  def getAngVel(): Double = ???
  def isSleeping(): js.Boolean = ???
  def isStatic(): js.Boolean = ???
  def isRogue(): js.Boolean = ???
  def setMass(mass: Double): Unit = ???
  def setMoment(moment: Double): Unit = ???
  def addShape(shape: Shape): Shape = ???
  def removeShape(shape: Shape): Unit = ???
  def removeConstraint(constraint: js.Any): Unit = ???
  def setPos(pos: Vect): Unit = ???
  def setVel(velocity: Vect): Unit = ???
  def setAngVel(w: Double): Unit = ???
  def setAngle(angle: Double): Unit = ???
  def applyForce(force: Vect, r: Vect): Unit = ???
  def applyImpulse(j: Vect, r: Vect): Unit = ???
  def getVelAtPoint(p: Vect): Vect = ???
}

@JSName("cp.CollisionHandler")
class CollisionHandler extends js.Object{
  var a: Shape = ???
  var b: Shape = ???
  var begin: (Arbiter, Space) => Boolean = ???
  var preSolve: (Arbiter, Space) => Boolean = ???
  var postSolve: (Arbiter, Space) => Unit = ???
  var separate: (Arbiter, Space) => Boolean = ???
}
@JSName("cp.Arbiter")
class Arbiter extends js.Object{
  def getShapes(): Seq[Shape] = ???
  def totalImpulse(): Vect = ???
  def totalImpulseWithFriction(): Vect = ???
  def totalKE(): Vect = ???
  def ignore(): Unit = ???
  def getA(): Shape = ???
  def getB(): Shape = ???
  def isFirstContact(): js.Boolean = ???
}
@JSName("cp.Space")
class Space() extends js.Object{
  def getCurrentTimeStep(): Double = ???
  def setIterations(iter: Double): Unit = ???
  def isLocked(): js.Boolean = ???
  def addCollisionHandler(a: js.Any, b: js.Any, preSolve: js.Function2[Arbiter, Space, Unit], postSolve: js.Function2[Arbiter, Space, Unit], separate: js.Function2[Arbiter, Space, Unit]): Unit = ???
  def removeCollisionHandler(a: js.Any, b: js.Any): Unit = ???
  def setDefaultCollisionHandler(preSolve: js.Any, postSolve: js.Any, separate: js.Any): Unit = ???
  def lookupHandler(a: js.Any, b: js.Any): Double = ???
  def addShape(shape: Shape): Shape = ???
  def addStaticShape(shape: Shape): Shape = ???
  def addBody(body: Body): Body = ???
  def addConstraint(constraint: Constraint): Constraint = ???
  def filterArbiters(body: Body, filters: js.Any): js.Any = ???
  def removeShape(shape: Shape): Unit = ???
  def removeStaticShape(shape: Shape): Unit = ???
  def removeBody(body: Body): Unit = ???
  def removeConstraint(constraint: js.Any): Unit = ???
  def containsShape(shape: Shape): js.Boolean= ???
  def containsBody(body: Body): js.Boolean= ???
  def containsConstraint(constaint: js.Any): js.Boolean = ???
  def step(dt: Double): Unit = ???

  def pointQuery(point: Vect, layers: Double, group: Double, func: js.Function1[Shape, Unit]): Unit = ???
  def pointQueryFirst(point: Vect, layers: Double, group: Double): Shape = ???
  def segmentQuery(start: Vect, end: Vect, layers: Double, group: Double, func: js.Function3[Shape, Double, Vect, Unit]): Unit = ???
  def segmentQueryFirst(start: Vect, end: Vect, layers: Double, group: Double): SegmentQueryInfo = ???
  def shapeQuery(shape: Shape, func: js.Function2[Shape, js.Any, Unit]): Unit = ???
  var damping: Double = ???
  var gravity: Vect = ???
  var staticBody: Body = ???
  var bodies: js.Array[Body] = ???
  var constraints: js.Array[Constraint] = ???
}
@JSName("cp.Constraint")
class Constraint(var a: Body, var b: Body) extends js.Object{
  def activateBodies(): js.Any = ???
  var maxForce: Double = ???
}
@JSName("cp.PinJoint")
class PinJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect) extends Constraint(a, b)
@JSName("cp.SlideJoint")
class SlideJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect, min: Double, max: Double) extends Constraint(a, b)
@JSName("cp.PivotJoint")
class PivotJoint(a: Body, b: Body, var anchr1: Vect, var anchr2: Vect) extends Constraint(a, b)
@JSName("cp.GrooveJoint")
class GrooveJoint(a: Body, b: Body, groove_a: Vect, groove_b: Vect, anchr2: Vect) extends Constraint(a, b)
@JSName("cp.DampedSpring")
class DampedSpring(a: Body, b: Body, anchr1: Vect, anchr2: Vect, restLength: Double, stiffness: Double, damping: Double) extends Constraint(a, b)
@JSName("cp.DampedRotarySpring")
class DampedRotarySpring(a: Body, b: Body, restAngle: Double, stiffness: Double, damping: Double) extends Constraint(a, b)
@JSName("cp.RotaryLimitJoint")
class RotaryLimitJoint(a: Body, b: Body, min: Double, max: Double) extends Constraint(a, b)
@JSName("cp.RatchetJoint")
class RatchetJoint(a: Body, b: Body, phase: js.Any, ratchet: js.Any) extends Constraint(a, b)
@JSName("cp.GearJoint")
class GearJoint(a: Body, b: Body, phase: js.Any, ratio: Double) extends Constraint(a, b)
@JSName("cp.SimpleMotor")
class SimpleMotor(a: Body, b: Body, rate: Double) extends Constraint(a, b)
