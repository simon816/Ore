package db.impl.action

import db.impl.pg.OrePostgresDriver.api._
import db.impl.{ProjectRoleTable, UserTable}
import db.{ModelActions, ModelService}
import models.user.User
import models.user.role.ProjectRole

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserActions(override val service: ModelService)
  extends ModelActions[UserTable, User](service, classOf[User], TableQuery[UserTable]) {

  /** The [[ModelActions]] for [[ProjectRole]]s. */
  val ProjectRoleActions = service.registry.registerActions(
    new ModelActions[ProjectRoleTable, ProjectRole](this.service, classOf[ProjectRole], TableQuery[ProjectRoleTable])
  )

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
