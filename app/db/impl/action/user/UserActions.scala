package db.impl.action.user

import db.ModelService
import db.action.ModelActions
import db.impl.OrePostgresDriver.api._
import db.impl.{ProjectRoleTable, UserTable}
import models.user.{ProjectRole, User}

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserActions(implicit val service: ModelService) extends ModelActions[UserTable, User](
  classOf[User], TableQuery[UserTable]) {

  val ProjectRoles = service.registrar.register(new ModelActions[ProjectRoleTable, ProjectRole](
    classOf[ProjectRole], TableQuery[ProjectRoleTable]
  ))

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
