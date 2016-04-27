package db.query.user

import db.OrePostgresDriver.api._
import db.orm.dao.ModelSet
import db.query.Queries
import db.{ProjectRolesTable, ProjectTable, UserTable}
import models.project.Project
import models.user.{ProjectRole, User}

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

  override def like(user: User): Future[Option[User]] = this.find(_.username.toLowerCase === user.username.toLowerCase)

}
