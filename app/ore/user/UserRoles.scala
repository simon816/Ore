package ore.user

import ore.Colors
import Colors._
import ore.Colors

/**
  * Represents a collection of roles a User may have.
  */
object UserRoles extends Enumeration {

  val SpongeLeader      =   new UserRole(0, 44,   4,  "Sponge Leader",        Amber)
  val TeamLeader        =   new UserRole(1, 58,   3,  "Team Leader",          Amber)
  val CommunityLeader   =   new UserRole(0, 59,   3,  "Community Leader",     Amber)
  val Staff             =   new UserRole(0, 3,    3,  "Sponge Staff",         Amber)
  val SpongeDev         =   new UserRole(3, 41,   3,  "Sponge Developer",     Green)
  val WebDev            =   new UserRole(2, 45,   3,  "Web Developer",        Blue)
  val Scribe            =   new UserRole(0, 51,   2,  "Sponge Documenter",    Aqua)
  val Support           =   new UserRole(4, 43,   2,  "Sponge Support",       Aqua)
  val Contributor       =   new UserRole(0, 49,   1,  "Sponge Contributor",   Green)
  val Adviser           =   new UserRole(5, 48,   1,  "Sponge Adviser",       Aqua)
  val DiamondDonor      =      new Donor(0, 52,       "Diamond Donor",       LightBlue)
  val GoldDonor         =      new Donor(0, 53,       "Gold Donor",          Gold)
  val IronDonor         =      new Donor(0, 56,       "Iron Donor",          Silver)
  val QuartzDonor       =      new Donor(0, 54,       "Quartz Donor",        Quartz)
  val StoneDonor        =      new Donor(0, 57,       "Stone Donor",         Gray)

  /**
    * Represents a User role.
    *
    * @param i            Index
    * @param id           ID of role
    * @param trustLevel   Level of trust that this user has
    * @param title        Title to display
    * @param color        Color to display
    */
  class UserRole(val           i: Int,
                 override val  id: Int,
                 val           trustLevel: Int,
                 val           title: String,
                 val           color: Color)
                 extends       super.Val(i, title)

  implicit def convert(value: Value): UserRole = value.asInstanceOf[UserRole]

}
