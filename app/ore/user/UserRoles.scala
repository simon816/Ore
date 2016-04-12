package ore.user

import ore.Colors._

/**
  * Represents a collection of roles a User may have.
  */
object UserRoles extends Enumeration {

  val SpongeLeader      =   new UserRole(  0, 44, 4,  "Sponge Leader",        Amber)
  val TeamLeader        =   new UserRole(  1, 58, 3,  "Team Leader",          Amber)
  val CommunityLeader   =   new UserRole(  2, 59, 3,  "Community Leader",     Amber)
  val Staff             =   new UserRole(  3, 3,  3,  "Sponge Staff",         Amber)
  val SpongeDev         =   new UserRole(  4, 41, 3,  "Sponge Developer",     Green)
  val WebDev            =   new UserRole(  5, 45, 3,  "Web Developer",        Blue)
  val Scribe            =   new UserRole(  6, 51, 2,  "Sponge Documenter",    Aqua)
  val Support           =   new UserRole(  7, 43, 2,  "Sponge Support",       Aqua)
  val Contributor       =   new UserRole(  8, 49, 1,  "Sponge Contributor",   Green)
  val Adviser           =   new UserRole(  9, 48, 1,  "Sponge Adviser",       Aqua)
  val DiamondDonor      =   new DonorRole(10, 52,     "Diamond Donor",        LightBlue)
  val GoldDonor         =   new DonorRole(11, 53,     "Gold Donor",           Gold)
  val IronDonor         =   new DonorRole(12, 56,     "Iron Donor",           Silver)
  val QuartzDonor       =   new DonorRole(13, 54,     "Quartz Donor",         Quartz)
  val StoneDonor        =   new DonorRole(14, 57,     "Stone Donor",          Gray)

  /**
    * Returns the role with the specified external ID.
    *
    * @param id   Sponge ID
    * @return     UserRole with ID
    */
  def withId(id: Int): UserRole = this.values.find(_.externalId == id).getOrElse {
    // Throw an exception instead of returning an Option to match Enumeration
    // behavior
    throw new NoSuchElementException
  }

  /**
    * Represents a User role.
    *
    * @param i            Index
    * @param externalId           ID of role
    * @param trustLevel   Level of trust that this user has
    * @param title        Title to display
    * @param color        Color to display
    */
  class UserRole(val i: Int,
                 val externalId: Int,
                 val trustLevel: Int,
                 val title: String,
                 val color: Color)
                 extends super.Val(i, title)

  implicit def convert(value: Value): UserRole = value.asInstanceOf[UserRole]

}
