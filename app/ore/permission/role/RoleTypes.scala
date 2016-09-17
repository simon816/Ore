package ore.permission.role

import models.user.role.ProjectRole
import ore.Colors._

/**
  * Represents a collection of roles a User may have.
  */
object RoleTypes extends Enumeration {

  // Global

  val Admin             =   new  RoleType( 0, 61,   Absolute,   "Ore Admin",             Red)
  val Mod               =   new  RoleType( 1, 62,   Standard,   "Ore Moderator",         Aqua)
  val SpongeLeader      =   new  RoleType( 2, 44,   Absolute,   "Sponge Leader",         Amber)
  val TeamLeader        =   new  RoleType( 3, 58,   Standard,   "Team Leader",           Amber)
  val CommunityLeader   =   new  RoleType( 4, 59,   Standard,   "Community Leader",      Amber)
  val Staff             =   new  RoleType( 5, 3,    Standard,   "Sponge Staff",          Amber)
  val SpongeDev         =   new  RoleType( 6, 41,   Standard,   "Sponge Developer",      Green)
  val WebDev            =   new  RoleType( 7, 45,   Standard,   "Web Developer",         Blue)
  val Scribe            =   new  RoleType( 8, 51,   Limited,    "Sponge Documenter",     Aqua)
  val Support           =   new  RoleType( 9, 43,   Limited,    "Sponge Support",        Aqua)
  val Contributor       =   new  RoleType(10, 49,   Default,    "Sponge Contributor",    Green)
  val Adviser           =   new  RoleType(11, 48,   Default,    "Sponge Adviser",        Aqua)
  val StoneDonor        =   new DonorType(12, 57,               "Stone Donor",           Gray)
  val QuartzDonor       =   new DonorType(13, 54,               "Quartz Donor",          Quartz)
  val IronDonor         =   new DonorType(14, 56,               "Iron Donor",            Silver)
  val GoldDonor         =   new DonorType(15, 53,               "Gold Donor",            Gold)
  val DiamondDonor      =   new DonorType(16, 52,               "Diamond Donor",         LightBlue)

  // Project

  val ProjectOwner      =   new  RoleType(17, -1,   Absolute,   "Owner",                 Transparent)
  val ProjectDev        =   new  RoleType(18, -2,   Standard,   "Developer",             Transparent)
  val ProjectEditor     =   new  RoleType(19, -3,   Limited,    "Editor",                Transparent)
  val ProjectSupport    =   new  RoleType(20, -4,   Default,    "Support",               Transparent)

  /**
    * Returns the role with the specified external ID.
    *
    * @param id Sponge ID
    * @return   UserRole with ID
    */
  def withId(id: Int): RoleType = this.values.find(_.roleId == id).getOrElse {
    // Throw an exception instead of returning an Option to match Enumeration
    // behavior
    throw new NoSuchElementException
  }

  /**
    * Returns the typical RoleTypes of [[ProjectRole]]s.
    *
    * @return RoleTypes used by ProjectRoles
    */
  def ofProjects = this.values.filter(_.roleId < 0)

  /**
    * Represents a User role.
    *
    * @param i      Index
    * @param roleId ID of role
    * @param trust  Level of trust that this user has
    * @param title  Title to display
    * @param color  Color to display
    */
  class RoleType(val i: Int,
                 val roleId: Int,
                 val trust: Trust,
                 val title: String,
                 val color: Color)
                 extends super.Val(i, title)

  /**
    * Represents a type of Donor.
    *
    * @param i      Index
    * @param roleId ID of role
    * @param title  Title to display
    * @param color  Color to display
    */
  class DonorType(override val i: Int,
                  override val roleId: Int,
                  override val title: String,
                  override val color: Color)
                  extends RoleType(i, roleId, Default, title, color)

  implicit def convert(value: Value): RoleType = value.asInstanceOf[RoleType]

}
