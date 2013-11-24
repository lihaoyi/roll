package example

import scala.scalajs.js

import example.cp.Implicits._
import scala.scalajs.extensions._
import scala.scalajs.js.SVGElement

case class Roll() extends Game {

  implicit val space = new cp.Space()
  space.damping = 0.95
  space.gravity = new cp.Vect(0, 400)

  val rock = Form.makePoly(
    points = Seq((1111500, 111300), (1111500, 111301), (1111501, 111300)),
    density = 1,
    static = false,
    friction = 0.6,
    elasticity = 0.6
  )

  val svg = new js.DOMParser().parseFromString(
    scala.js.resource.apply("Demo.svg").string,
    "text/xml"
  )

  val static =
    svg.getElementById("Static")
       .children
       .map(Form.processElement(_, static = true))

  val dynamic =
    svg.getElementById("Dynamic")
       .children
       .map(Form.processElement(_, static = false))


  val backgroundImg = js.globals.document.createElement("img").asInstanceOf[js.HTMLImageElement]
  val dataString = s"<svg xmlns='http://www.w3.org/2000/svg' width='2000' height='2000'>" +
    new js.XMLSerializer().serializeToString(svg.getElementById("Background")) +
    "</svg>"

  backgroundImg.src = "data:image/svg+xml;base64," + js.globals.btoa(dataString)



  val staticJoints =
    svg.getElementById("Joints")
       .children
       .map(Form.processJoint)
       .flatten

  val player =
    Form.processElement(svg.getElementById("Player"), static = false)

  js.Dynamic.global.svg = svg.getElementById("Background")
  val strokes = new Strokes(space)

  def cameraPos = {
    player.body.getPos() + player.body.getVel()
  }
  def drawStatic(ctx: js.CanvasRenderingContext2D, w: Int, h: Int) = {
    strokes.drawStatic(ctx, w, h)

  }
  def draw(ctx: js.CanvasRenderingContext2D) = {

    ctx.drawImage(backgroundImg, 0, 0)
    for(form <- static ++ dynamic :+ player if form != null){
      val body = form.body
      ctx.save()
      ctx.lineWidth = 3
      ctx.strokeStyle = form.strokeStyle
      ctx.fillStyle = form.fillStyle
      ctx.translate(
        body.getPos().x,
        body.getPos().y
      )

      ctx.rotate(body.a)
      form.drawable match{
        case Drawable.Circle(r) =>
          ctx.fillCircle(0, 0, r)
          ctx.strokeCircle(0, 0, r)
          ctx.strokePathOpen((0, r/1.5), (0, r))

        case Drawable.Polygon(pts) =>

          ctx.fillPath(pts: _*)
          ctx.strokePath(pts: _*)
      }
      ctx.restore()
    }

    staticJoints.foreach{ jform  =>
        ctx.save()
        ctx.fillStyle = jform.fillStyle
        ctx.strokeStyle = jform.strokeStyle
        ctx.translate(jform.joint.a.getPos().x, jform.joint.a.getPos().y)
        ctx.rotate(jform.joint.a.a)
        ctx.fillCircle(jform.joint.anchr1.x, jform.joint.anchr1.y, 5)
        ctx.restore()
    }

    strokes.draw(ctx)
  }

  def update(keys: Set[Int], lines: Seq[(cp.Vect, cp.Vect)], touching: Boolean) = {

    val baseT = 0.45
    val maxW = 25
    val decay = (maxW - baseT) / maxW
    if (keys(KeyCode.left)) {
      player.body.w = (player.body.w - baseT) * decay
    }
    if (keys(KeyCode.right)){
      player.body.w = (player.body.w + baseT) * decay
    }
    strokes.update(lines, touching)

    space.step(1.0/60)
  }
}

class Strokes(space: cp.Space){
  var duration = 1500
  var max = 600.0
  var remaining = max
  var regenRate = 2.0
  var delayMax = 60
  var delay = delayMax
  var strokes = Seq.empty[(cp.SegmentShape, Long)]
  
  def drawStatic(ctx: js.CanvasRenderingContext2D, w: Int, h: Int) = {
    ctx.fillStyle = Color.Cyan.toString
    ctx.fillRect(0, h - 10, w * 1.0 * remaining / max, 10)
  }

  def draw(ctx: js.CanvasRenderingContext2D) = {
    ctx.strokeStyle = Color.Cyan.toString
    ctx.lineWidth = 3
    strokes.foreach{ case (first, dur) =>
      ctx.strokePathOpen(first.a, first.b)
    }
  }
  def update(lines: Seq[(cp.Vect, cp.Vect)], touching: Boolean) = {
    val (liveStrokes, deadStrokes) = strokes.partition{
      case (s, t) => t + duration > System.currentTimeMillis()
    }
    def hitDynamicShape(p1: cp.Vect, p2: cp.Vect) = {
      val shapes = collection.mutable.Buffer.empty[cp.Shape]
      space.segmentQuery(p1, p2, ~0, 0, {(s: cp.Shape) => shapes += s; ()})
      space.pointQuery(p1, ~0, 0, {(s: cp.Shape) => shapes += s; ()})
      space.pointQuery(p2, ~0, 0, {(s: cp.Shape) => shapes += s; ()})
      shapes.exists(!_.getBody().isStatic())
    }
    val newStrokes = for {
      (p1, p2) <- lines
      if remaining > 0
      d = p2 - p1
      lengthLeft = math.min(remaining, d.length)
      p3 = d * lengthLeft / d.length + p1
      if !hitDynamicShape(p1, p3)
    } yield {
      delay = delayMax

      remaining -= lengthLeft
      val shape = new cp.SegmentShape(
        space.staticBody,
        (p1.x, p1.y),
        (p3.x, p3.y),
        0
      )
      space.addShape(shape)

      shape.setFriction(0.6)
      shape.setElasticity(0.1)
      shape -> System.currentTimeMillis()
    }
    if (!touching && remaining < max) {
      if (delay > 0) delay -= 1
      else remaining += regenRate
    }

    deadStrokes.map(_._1).foreach(space.removeShape)
    strokes = newStrokes ++ liveStrokes
  }
}