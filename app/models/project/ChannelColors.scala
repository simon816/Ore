package models.project

/**
  * Represents a color that a Channel may be represented by.
  */
object ChannelColors extends Enumeration {

  val Purple      =   ChannelColor(0,    "#B400FF")
  val Violet      =   ChannelColor(1,    "#C87DFF")
  val Magenta     =   ChannelColor(2,    "#E100E1")
  val Blue        =   ChannelColor(3,    "#0000FF")
  val Aqua        =   ChannelColor(4,    "#0096FF")
  val Cyan        =   ChannelColor(5,    "#00E1E1")
  val Green       =   ChannelColor(6,    "#00DC00")
  val DarkGreen   =   ChannelColor(7,    "#009600")
  val Chartreuse  =   ChannelColor(8,    "#7FFF00")
  val Amber       =   ChannelColor(9,    "#FFC800")
  val Orange      =   ChannelColor(10,   "#FF8200")
  val Red         =   ChannelColor(11,   "#DC0000")

  case class ChannelColor(i: Int, hex: String) extends super.Val(i, hex)
  implicit def convert(value: Value): ChannelColor = value.asInstanceOf[ChannelColor]

}
