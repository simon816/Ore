package db.query

import db.OrePostgresDriver.api._
import Queries._
import db.UserTable
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

  /**
    * Creates a new User.
    *
    * @param user User to create
    */
  def create(user: User) = {
    val users = q[UserTable](classOf[User])
    val action = DBIO.seq(users += user)
    DB.run(action)
  }

  /**
    * Returns the specified User or creates it if it doesn't exist.
    *
    * @param user   User to get or create
    * @return       New or existing User
    */
  def getOrCreate(user: User): User = {
    now(withName(user.username)).get.getOrElse {
      now(create(user)).get
      user
    }
  }

}
