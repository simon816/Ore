package models.project

/**
  * Represents a color that a Channel may be represented by.
  */
object ChannelColors extends Enumeration {

  val Purple      =   ChannelColor(0,    "#B400FF",   isDark = true   )
  val Violet      =   ChannelColor(1,    "#C87DFF",   isDark = false  )
  val Magenta     =   ChannelColor(2,    "#E100E1",   isDark = true   )
  val Blue        =   ChannelColor(3,    "#0000FF",   isDark = true   )
  val Aqua        =   ChannelColor(4,    "#0096FF",   isDark = false  )
  val Cyan        =   ChannelColor(5,    "#00E1E1",   isDark = false  )
  val Green       =   ChannelColor(6,    "#00DC00",   isDark = false  )
  val DarkGreen   =   ChannelColor(7,    "#009600",   isDark = true   )
  val Yellow      =   ChannelColor(8,    "#FFFF32",   isDark = false  )
  val Amber       =   ChannelColor(9,    "#FFC800",   isDark = false  )
  val Orange      =   ChannelColor(10,   "#FF8200",   isDark = true   )
  val Red         =   ChannelColor(11,   "#DC0000",   isDark = true   )

  case class ChannelColor(i: Int, hex: String, isDark: Boolean) extends super.Val(i, hex)
  implicit def convert(value: Value): ChannelColor = value.asInstanceOf[ChannelColor]

}
