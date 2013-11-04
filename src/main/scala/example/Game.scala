package example

import scala.scalajs.js

import ScalaJSExample.pimpedContext
import example.chipmunk._

import EasySeq._
import scala.scalajs.js.Element

object Forms{
  def makeRect(pos: Point,
               dims: Point,
               density: Double,
               static: Boolean,
               friction: Double = 0.3,
               elasticity: Double = 0.3)
              (implicit space: Space) = {
    makePoly(
      Seq(
        pos,
        pos + (0, dims.y),
        pos + dims,
        pos + (dims.x, 0)
      ),
      density,
      static,
      friction,
      elasticity
    )
  }

  def makePoly(points: Seq[Point],
               density: Double,
               static: Boolean,
               friction: Double,
               elasticity: Double)
              (implicit space: Space) = {
    val flatPoints: js.Array[Num] = points.flatMap(p => Seq(p.x: Num, p.y: Num)).toArray[Num]
    val center = cp.centroidForPoly(flatPoints)

    cp.recenterPoly(flatPoints)
    val area = cp.areaForPoly(flatPoints)
    val mass = density * area

    if (static) {
      for(i <- 0 until points.length){
        val p1 = points(i)
        val p2 = points((i + 1) % points.length)
        val shape = new SegmentShape(
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
        new Body(mass, cp.momentForPoly(mass, flatPoints, (0, 0)))
      )
      body.setPos(center)
      val shape = space.addShape(
        new PolyShape(body, flatPoints, (0, 0))
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
                    (implicit space: Space) = {

    elem.nodeName.toString match{
      case "rect" =>
        val Seq(x, y, w, h) = Seq("x", "y", "width", "height").map(c =>
          elem.getAttribute(c).toString.toInt
        )
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
              .map(s => s.split(","))
              .map(p => Point(p(0).toDouble, p(1).toDouble))

        Forms.makePoly(
          points,
          density,
          static,
          friction,
          elasticity
        )

      case _ =>
    }
  }
}


case class Tetris() extends Game {

  implicit val space = new Space()

  space.gravity = new Vect(0, 500)

  val rock = Forms.makeRect(
    pos = (500, 300),
    dims = (50, 50),
    density = 1,
    static = false
  )
  rock.setVel((400, 0))

  val svg = new js.DOMParser().parseFromString(
    js.Resource("Blocks.svg").string,
    "text/xml"
  )


  val static =
    svg.getElementById("Static")
       .children
       .foreach(Forms.processElement(_, density = 1, friction = 0.3, elasticity = 0.3, static = true))
  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .foreach(Forms.processElement(_, density = 1, friction = 0.3, elasticity = 0.3, static = false))


  def draw(ctx: js.CanvasRenderingContext2D) = {


    for(rect <- space.bodies :+ space.staticBody){
      ctx.save()

      ctx.translate(
        rect.getPos().x,
        rect.getPos().y
      )

      ctx.rotate(rect.a)
      rect.shapeList.foreach{
        case shape: PolyShape =>
          ctx.strokeStyle = Color.Red
          val nums = shape.verts
          ctx.strokePath(
            nums.toSeq
                .grouped(2)
                .map{case Seq(x, y) => Point(x, y)}
                .toSeq:_*
          )
        case shape: SegmentShape =>
          ctx.strokeStyle = Color.Black
          ctx.strokePath(
            Point(shape.a.x, shape.a.y),
            Point(shape.b.x, shape.b.y)
          )

      }

      ctx.restore()
    }
  }

  def update(keys: Set[Int]) = {
    space.step(1.0/60)
  }
}