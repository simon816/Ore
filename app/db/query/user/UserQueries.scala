package db.query.user

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.UserTable
import db.query.Queries
import models.user.User

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserQueries extends Queries[UserTable, User](TableQuery(tag => new UserTable(tag))) {

  val ProjectRoles = new ProjectRolesQueries

  /**
    * Returns the User with the specified username.
    *
    * @param username   Username to find
    * @return           User if found, None otherwise
    */
  def withName(username: String): Future[Option[User]] = {
    ?(_.username === username)
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], user: User): User = {
    user.copy(createdAt = theTime)
  }

  override def named(user: User): Future[Option[User]] = withName(user.username)

}
