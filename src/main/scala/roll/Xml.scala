package roll

import org.scalajs.dom
import org.scalajs.dom.extensions._
import cp.Implicits._
import roll.cp.Cp

/**
 * A basic AST for the SVG documents that represent the levels in Roll.
 *
 * Collapses Rectangles into generic Polygons
 *
 * Records each shape together with some Misc information common to all
 * shapes.
 */
sealed trait Xml{
  def get(s: String) =
    this.cast[Xml.Group]
      .children
      .find(_.misc.id == s)

  def apply(s: String) = get(s).get
  def children: Seq[Xml] = {
    this.cast[Xml.Group]
        .children
  }
  def misc: Xml.Misc
}
object Xml {
  case class Misc(id: String, fill: String, stroke: String)
  case class Circle(x: Double, y: Double, r: Double, misc: Misc) extends Xml
  case class Line(x1: Double, y1: Double, x2: Double, y2: Double, misc: Misc) extends Xml
  case class Polygon(pts: Seq[(Double, Double)], misc: Misc) extends Xml
  case class Group(override val children: Seq[Xml], misc: Misc) extends Xml

  def parse(el: dom.Element): Seq[Xml] = {
    def s(s: String, default: String = "") = Option(el.getAttribute(s)).getOrElse(default)
    def d(s: String, default: Double = 0.0) = Option(el.getAttribute(s)).fold(default)(_.toDouble)
    val misc = Misc(s("id"), s("fill"), s("stroke"))
    el.tagName match {
      case "circle" => Circle(d("cx"), d("cy"), d("r"), misc) :: Nil
      case "rect" =>

        val transforms = s("transform")
        val (x, y, w, h) = (d("x"), d("y"), d("width"), d("height"))
        val points = Seq[(Double, Double)](
          (x, y),
          (x, y + h),
          (x + w, y + h),
          (x + w, y)
        )
        val transformedPoints: Seq[(Double, Double)] =
          if (transforms == "") points
          else {
            val svg =
              dom.document
                .createElementNS("http://www.w3.org/2000/svg", "svg")
                .cast[dom.SVGSVGElement]

            val transforms =
              el.cast[dom.SVGRectElement]
                .transform
                .baseVal

            var svgPt = svg.createSVGPoint()

            points.map { p =>
              svgPt.x = p._1
              svgPt.y = p._2
              for (transform <- transforms) {
                svgPt = svgPt.matrixTransform(transform.matrix)
              }
              (svgPt.x, svgPt.y)
            }

          }
        val flat = transformedPoints.flatMap(p => Seq(p._1, p._2)).toArray
        println(
          Cp.areaForPoly(flat)
        )
        Polygon(transformedPoints, misc) :: Nil

      case "line" => Line(d("x1"), d("y1"), d("x2"), d("y2"), misc) :: Nil
      case "polyline" | "polygon" =>
        val pts = s("points")
          .toString
          .split("\\s+")
          .toSeq
          .map(_.split(","))
          .map{ case Array(x, y) => (x.toDouble, y.toDouble)}

        Polygon(pts, misc) :: Nil
      case "g" | "svg" =>
        Group(el.children.flatMap(parse), misc) :: Nil
      case _ =>
        Nil
    }
  }

}
