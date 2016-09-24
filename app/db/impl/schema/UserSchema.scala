package db.impl.schema

import db.impl.OrePostgresDriver.api._
import db.impl.{ProjectRoleTable, UserTable}
import db.{ModelSchema, ModelService}
import models.user.User
import models.user.role.ProjectRole

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserSchema(override val service: ModelService)
  extends ModelSchema[User](service, classOf[User], TableQuery[UserTable]) {

  /** The [[ModelSchema]] for [[ProjectRole]]s. */
  val ProjectRoleActions = service.registry.registerSchema(
    new ModelSchema[ProjectRole](this.service, classOf[ProjectRole], TableQuery[ProjectRoleTable])
  )

  override def like(user: User): Future[Option[User]]
  = this.service.find[User](this.modelClass, _.username.toLowerCase === user.username.toLowerCase)

}
