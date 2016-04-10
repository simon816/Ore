package ore

/**
  * Represents a color that a Channel may be represented by.
  */
object Colors extends Enumeration {

  val Purple      =   Color(0,    "#B400FF")
  val Violet      =   Color(1,    "#C87DFF")
  val Magenta     =   Color(2,    "#E100E1")
  val Blue        =   Color(3,    "#0000FF")
  val Aqua        =   Color(4,    "#0096FF")
  val Cyan        =   Color(5,    "#00E1E1")
  val Green       =   Color(6,    "#00DC00")
  val DarkGreen   =   Color(7,    "#009600")
  val Chartreuse  =   Color(8,    "#7FFF00")
  val Amber       =   Color(9,    "#FFC800")
  val Orange      =   Color(10,   "#FF8200")
  val Red         =   Color(11,   "#DC0000")

  case class Color(i: Int, hex: String) extends super.Val(i, hex)
  implicit def convert(value: Value): Color = value.asInstanceOf[Color]

}
