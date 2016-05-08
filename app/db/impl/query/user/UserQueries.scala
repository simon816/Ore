package db.impl.query.user

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.query.ModelQueries
import db.impl.{ProjectRoleTable, UserTable}
import models.user.{ProjectRole, User}

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserQueries(implicit val service: ModelService) extends ModelQueries[UserTable, User](
  classOf[User], TableQuery[UserTable]) {

  val ProjectRoles = service.registrar.register(new ModelQueries[ProjectRoleTable, ProjectRole](
    classOf[ProjectRole], TableQuery[ProjectRoleTable]
  ))

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
