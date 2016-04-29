package db.query.user

import db.OrePostgresDriver.api._
import db.orm.dao.ChildModelSet
import db.query.ModelQueries
import db.query.ModelQueries.run
import db.{FlagTable, ProjectRoleTable, ProjectTable, UserTable}
import models.project.{Flag, Project}
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes.RoleType

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

  def setGlobalRoles(user: User, globalRoles: List[RoleType])
  = run((for { model <- this.baseQuery if model.id === user.id.get } yield model.globalRoles).update(globalRoles))

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
