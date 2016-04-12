package models.project.author

import models.user.User

/**
  * Represents a single developer on a project. A developer is not necessarily
  * registered with Ore.
  *
  * @param name Name of developer
  */
case class Dev(override val name: String) extends Author {

  /**
    * Tries to resolve this Dev to a registered User.
    *
    * @return User if present, None otherwise
    */
  def user: Option[User] = User.withName(this.name)

}
