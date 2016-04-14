package ore.permission.role

import ore.Colors._

/**
  * Represents a collection of roles a User may have.
  */
object RoleTypes extends Enumeration {

  val SpongeLeader      =   new  RoleType( 0, 44,   Absolute,   "Sponge Leader",         Amber)
  val TeamLeader        =   new  RoleType( 1, 58,   Standard,   "Team Leader",           Amber)
  val CommunityLeader   =   new  RoleType( 2, 59,   Standard,   "Community Leader",      Amber)
  val Staff             =   new  RoleType( 3, 3,    Standard,   "Sponge Staff",          Amber)
  val SpongeDev         =   new  RoleType( 4, 41,   Standard,   "Sponge Developer",      Green)
  val WebDev            =   new  RoleType( 5, 45,   Standard,   "Web Developer",         Blue)
  val Scribe            =   new  RoleType( 6, 51,   Limited,    "Sponge Documenter",     Aqua)
  val Support           =   new  RoleType( 7, 43,   Limited,    "Sponge Support",        Aqua)
  val Contributor       =   new  RoleType( 8, 49,   Default,    "Sponge Contributor",    Green)
  val Adviser           =   new  RoleType( 9, 48,   Default,    "Sponge Adviser",        Aqua)
  val DiamondDonor      =   new DonorType(10, 52,               "Diamond Donor",         LightBlue)
  val GoldDonor         =   new DonorType(11, 53,               "Gold Donor",            Gold)
  val IronDonor         =   new DonorType(12, 56,               "Iron Donor",            Silver)
  val QuartzDonor       =   new DonorType(13, 54,               "Quartz Donor",          Quartz)
  val StoneDonor        =   new DonorType(14, 57,               "Stone Donor",           Gray)

  val ProjectOwner      =   new RoleType(15, -1,    Absolute,   "Project Owner",         Transparent)

  /**
    * Returns the role with the specified external ID.
    *
    * @param id   Sponge ID
    * @return     UserRole with ID
    */
  def withId(id: Int): RoleType = this.values.find(_.roleId == id).getOrElse {
    // Throw an exception instead of returning an Option to match Enumeration
    // behavior
    throw new NoSuchElementException
  }

  /**
    * Represents a User role.
    *
    * @param i            Index
    * @param roleId       ID of role
    * @param trust        Level of trust that this user has
    * @param title        Title to display
    * @param color        Color to display
    */
  class RoleType(val i: Int,
                 val roleId: Int,
                 val trust: Trust,
                 val title: String,
                 val color: Color)
                 extends super.Val(i, title)

  class DonorType(override val i: Int,
                  override val roleId: Int,
                  override val title: String,
                  override val color: Color)
                  extends RoleType(i, roleId, Default, title, color)

  implicit def convert(value: Value): RoleType = value.asInstanceOf[RoleType]

}
