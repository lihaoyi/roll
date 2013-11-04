package example.chipmunk

import scala.scalajs.js
import js.annotation.JSName
object cp extends js.Object{
  def momentForBox(m: Num, width: Num, height: Num): Num = ???
  def areaForPoly(verts: js.Array[Num]): Num = ???
  def momentForPoly(m: Num, verts: js.Array[Num], offset: Vect): Num = ???
  def centroidForPoly(verts: js.Array[Num]): Vect = ???
  def recenterPoly(verts: js.Array[Num]): Unit = ???
}
@JSName("cp.Vect")
class Vect(var x: Num, var y: Num) extends js.Object
@JSName("cp.BB")
class BB(var l: Num, var b: Num, var r: Num, var t: Num)

@JSName("cp.Shape")
class Shape(body: Body) extends js.Object{
  def setElasticity(e: Num): Unit = ???
  def setFriction(u: Num): Unit = ???
  def setLayers(layers: js.Any): Unit = ???
  def setSensor(sensor: js.Any): Unit = ???
  def setCollisionType(collisionType: js.Any): Unit = ???
  def getBody(): Body = ???
  def active(): js.Boolean = ???
  def setBody(b: Body) = ???
  def cacheBB(): js.Any = ???
  def update(pos: Vect, rot: Num): js.Any = ???
}

@JSName("cp.PointQueryExtendedInfo")
class PointQueryExtendedInfo(var shape: Shape) extends js.Object
@JSName("cp.NearestPointQueryInfo")
class NearestPointQueryInfo(var shape: Shape, p: Vect, d: Num) extends js.Object
@JSName("cp.SegmentQueryInfo")
class SegmentQueryInfo(var shape: Shape, t: Num, n: Vect) extends js.Object
@JSName("cp.CircleShape")
class CircleShape(body: Body, radius: Num, offset: Vect) extends Shape(body)
@JSName("cp.SegmentShape")
class SegmentShape(body: Body, var a: Vect, var b: Vect, r: Num) extends Shape(body)
@JSName("cp.PolyShape")
class PolyShape(body: Body, var verts: js.Array[Num], var offset: Vect) extends Shape(body)

@JSName("cp.BoxShape")
object BoxShape extends js.Object{
  def apply(body: Body, width: Num, height: Num): PolyShape = ???
}
@JSName("cp.Body")
class Body(val m: Num, val i: Num) extends js.Object{
  var a: Num = ???
  var shapeList: js.Array[Shape] = ???
  def getPos(): Vect = ???
  def getVel(): Vect = ???
  def getAngVel(): Num = ???
  def isSleeping(): js.Boolean = ???
  def isStatic(): js.Boolean = ???
  def isRogue(): js.Boolean = ???
  def setMass(mass: Num): Unit = ???
  def setMoment(moment: Num): Unit = ???
  def addShape(shape: Shape): Shape = ???
  def removeShape(shape: Shape): Unit = ???
  def removeConstraint(constraint: js.Any): Unit = ???
  def setPos(pos: Vect): Unit = ???
  def setVel(velocity: Vect): Unit = ???
  def setAngVel(w: Num): Unit = ???
  def setAngle(angle: Num): Unit = ???
  def applyForce(force: Vect, r: Vect): Unit = ???
  def applyImpulse(j: Vect, r: Vect): Unit = ???
  def getVelAtPoint(p: Vect): Vect = ???
}
@JSName("cp.Space")
class Space() extends js.Object{
  def getCurrentTimeStep(): Num = ???
  def setIterations(iter: Num): Unit = ???
  def isLocked(): js.Boolean = ???
  def addCollisionHandler(a: js.Any, b: js.Any, preSolve: js.Any, postSolve: js.Any, separate: js.Any): Num = ???
  def removeCollisionHandler(a: js.Any, b: js.Any): Num = ???
  def setDefaultCollisionHandler(a: js.Any, b: js.Any, preSolve: js.Any, postSolve: js.Any, separate: js.Any): Num = ???
  def lookupHandler(a: js.Any, b: js.Any): Num = ???
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
  def step(dt: Num): Unit = ???
  var gravity: Vect = ???
  var staticBody: Body = ???
  var bodies: js.Array[Body] = ???
}
@JSName("cp.Constraint")
class Constraint(a: Body, b: Body) extends js.Object{
  def activateBodies(): js.Any = ???
}
@JSName("cp.PinJoint")
class PinJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect) extends Constraint(a, b)
@JSName("cp.SlideJoint")
class SlideJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect, min: Num, max: Num) extends Constraint(a, b)
@JSName("cp.PivotJoint")
class PivotJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect) extends Constraint(a, b)
@JSName("cp.GrooveJoint")
class GrooveJoint(a: Body, b: Body, groove_a: Vect, groove_b: Vect, anchr2: Vect) extends Constraint(a, b)
@JSName("cp.DampedSpring")
class DampedSpring(a: Body, b: Body, anchr1: Vect, anchr2: Vect, restLength: Num, stiffness: Num, damping: Num) extends Constraint(a, b)
@JSName("cp.DampedRotarySpring")
class DampedRotarySpring(a: Body, b: Body, anchr1: Vect, anchr2: Vect, restAngle: Num, stiffness: Num, damping: Num) extends Constraint(a, b)
@JSName("cp.RotaryLimitJoint")
class RotaryLimitJoint(a: Body, b: Body, min: Num, max: Num) extends Constraint(a, b)
@JSName("cp.RatchetJoint")
class RatchetJoint(a: Body, b: Body, phase: js.Any, ratchet: js.Any) extends Constraint(a, b)
@JSName("cp.GearJoint")
class GearJoint(a: Body, b: Body, phase: js.Any, ratio: Num) extends Constraint(a, b)
@JSName("cp.SimpleMotor")
class SimpleMotor(a: Body, b: Body, rate: Num) extends Constraint(a, b)
