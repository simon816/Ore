package ore

import ore.Colors.Color

/**
  * Represents a collection of roles a User may have.
  */
object UserRoles extends Enumeration {

  val Staff       =   UserRole(0,   "Sponge Staff",       Colors.Amber)
  val SpongeDev   =   UserRole(1,   "Sponge Developer",   Colors.Green)
  val PluginDev   =   UserRole(2,   "Plugin Developer",   Colors.Magenta)
  val SpongeLeader=   UserRole(3,   "Sponge Leader",      Colors.Red)

  case class UserRole(i: Int, title: String, color: Color) extends super.Val(i, title)
  implicit def convert(value: Value): UserRole = value.asInstanceOf[UserRole]

}
