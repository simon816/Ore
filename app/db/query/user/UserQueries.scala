package db.query.user

import db.UserTable
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import models.user.User

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserQueries extends ModelQueries {

  override type Row = User
  override type Table = UserTable

  val ProjectRoles = new ProjectRolesQueries

  override val modelClass = classOf[User]
  override val baseQuery = TableQuery[UserTable]

  registerModel()

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
