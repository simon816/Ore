package db.query.impl.user

import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import db.query.ModelQueries.registrar
import db.{ProjectRoleTable, UserTable}
import models.user.{ProjectRole, User}

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserQueries extends ModelQueries[UserTable, User](classOf[User], TableQuery[UserTable]) {

  val ProjectRoles = registrar.register(new ModelQueries[ProjectRoleTable, ProjectRole](
    classOf[ProjectRole], TableQuery[ProjectRoleTable]
  ))

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
