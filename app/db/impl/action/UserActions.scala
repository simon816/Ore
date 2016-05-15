package db.impl.action

import db.ModelService
import db.action.ModelActions
import db.impl.OrePostgresDriver.api._
import db.impl.{ProjectRoleTable, UserTable}
import models.user.{ProjectRole, User}

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserActions(override val service: ModelService)
  extends ModelActions[UserTable, User](service, classOf[User], TableQuery[UserTable]) {

  /** The [[ModelActions]] for [[ProjectRole]]s. */
  val ProjectRoleActions = service.registrar.register(
    new ModelActions[ProjectRoleTable, ProjectRole](this.service, classOf[ProjectRole], TableQuery[ProjectRoleTable])
  )

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
