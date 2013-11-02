package example.chipmunk

import scala.scalajs.js


class Vect(var x: Num, var y: Num) extends js.Object
class BB(var l: Num, var b: Num, var r: Num, var t: Num)

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
class PointQueryExtendedInfo(var shape: Shape) extends js.Object
class NearestPointQueryInfo(var shape: Shape, p: Vect, d: Num) extends js.Object
class SegmentQueryInfo(var shape: Shape, t: Num, n: Vect) extends js.Object
class CircleShape(body: Body, radius: Num, offset: Vect) extends Shape(body)
class SegmentShape(body: Body, a: Vect, b: Vect, r: Num) extends Shape(body)
class PolyShape(body: Body, var verts: js.Array[Num], var offset: Vect) extends Shape(body)
object BoxShape extends js.Object{
  def apply(body: Body, width: Num, height: Num): PolyShape = ???
}
class Body(m: Num, i: Num) extends js.Object{
  def getPos(): Vect = ???
  def getVel(): Vect = ???
  def getAngleVel(): Num = ???
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
  def setAngleVel(w: Num): Unit = ???
  def setAngle(angle: Num): Unit = ???
  def applyForce(force: Vect, r: Vect): Unit = ???
  def applyImpulse(j: Vect, r: Vect): Unit = ???
  def getVelAtPoint(p: Vect): Vect = ???
}
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
}
class Constraint(a: Body, b: Body) extends js.Object{
  def activateBodies(): js.Any = ???
}
class PinJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect) extends Constraint(a, b)
class SlideJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect, min: Num, max: Num) extends Constraint(a, b)
class PivotJoint(a: Body, b: Body, anchr1: Vect, anchr2: Vect) extends Constraint(a, b)
class GrooveJoint(a: Body, b: Body, groove_a: Vect, groove_b: Vect, anchr2: Vect) extends Constraint(a, b)
class DampedSpring(a: Body, b: Body, anchr1: Vect, anchr2: Vect, restLength: Num, stiffness: Num, damping: Num) extends Constraint(a, b)
class DampedRotarySpring(a: Body, b: Body, anchr1: Vect, anchr2: Vect, restAngle: Num, stiffness: Num, damping: Num) extends Constraint(a, b)
class RotaryLimitJoint(a: Body, b: Body, min: Num, max: Num) extends Constraint(a, b)
class RatchetJoint(a: Body, b: Body, phase: js.Any, ratchet: js.Any) extends Constraint(a, b)
class GearJoint(a: Body, b: Body, phase: js.Any, ratio: Num) extends Constraint(a, b)
class SimpleMotor(a: Body, b: Body, rate: Num) extends Constraint(a, b)
