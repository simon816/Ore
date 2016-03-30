package models.author

import db.Storage
import models.auth.User

import scala.util.{Failure, Success}

/**
  * Represents a single developer on a project. A developer is not necessarily
  * registered with Ore.
  *
  * @param name Name of developer
  */
case class Dev(override val name: String) extends AbstractAuthor {

  /**
    * Tries to resolve this Dev to a registered User.
    *
    * @return User if present, None otherwise
    */
  def getUser: Option[User] = {
    Storage.now(Storage.optUser(name)) match {
      case Failure(thrown) => throw thrown
      case Success(userOpt) => userOpt
    }
  }

}
