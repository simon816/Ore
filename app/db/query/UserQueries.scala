package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.UserTable
import db.query.Queries._
import models.auth.User

import scala.concurrent.Future

/**
  * User related queries.
  */
object UserQueries extends ModelQueries[UserTable, User] {

  /**
    * Returns the User with the specified username.
    *
    * @param username   Username to find
    * @return           User if found, None otherwise
    */
  def withName(username: String): Future[Option[User]] = {
    find[UserTable, User](classOf[User], u => u.username === username)
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], user: User): User = {
    user.copy(externalId = id.getOrElse(user.externalId), createdAt = theTime)
  }

  override def named(user: User): Future[Option[User]] = withName(user.username)

}
