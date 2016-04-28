package db.query.user

import db.OrePostgresDriver.api._
import db.orm.dao.ModelSet
import db.query.Queries
import db.query.Queries.run
import db.{FlagTable, ProjectRolesTable, ProjectTable, UserTable}
import models.project.{Flag, Project}
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes.RoleType

import scala.concurrent.Future

/**
  * User related queries.
  */
class UserQueries extends Queries {

  override type Row = User
  override type Table = UserTable

  val ProjectRoles = new ProjectRolesQueries

  override val modelClass = classOf[User]
  override val baseQuery = TableQuery[UserTable]

  registerModel()

  /**
    * Returns the user's projects.
    *
    * @param user User to get projects for
    * @return     User projects
    */
  def getProjects(user: User): ModelSet[UserTable, User, ProjectTable, Project]
  = Queries.getModelSet[UserTable, User, ProjectTable, Project](classOf[Project], _.ownerId, user)

  /**
    * Returns the ProjectRoles for all Projects this user is in.
    *
    * @param user User to get roles for
    * @return ProjectRoles user has
    */
  def getProjectRoles(user: User): ModelSet[UserTable, User, ProjectRolesTable, ProjectRole]
  = Queries.getModelSet[UserTable, User, ProjectRolesTable, ProjectRole](classOf[ProjectRole], _.userId, user)

  /**
    * Returns the [[Flag]]s submitted by the user.
    *
    * @param user User to get flags for
    * @return     Flags submitted by user
    */
  def getFlags(user: User): ModelSet[UserTable, User, FlagTable, Flag]
  = Queries.getModelSet[UserTable, User, FlagTable, Flag](classOf[Flag], _.userId, user)

  def setGlobalRoles(user: User, globalRoles: List[RoleType])
  = run((for { model <- this.baseQuery if model.id === user.id.get } yield model.globalRoles).update(globalRoles))

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
