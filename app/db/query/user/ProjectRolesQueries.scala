package db.query.user

import db.OrePostgresDriver.api._
import db.ProjectRolesTable
import db.query.Queries
import db.query.Queries.run
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes.RoleType

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class ProjectRolesQueries extends Queries {

  override type Row = ProjectRole
  override type Table = ProjectRolesTable

  override val modelClass = classOf[ProjectRole]
  override val baseQuery = TableQuery[ProjectRolesTable]

  registerModel()

  /**
    * Returns all the users that have one or more roles in the specified
    * Project.
    *
    * @param projectId  Project to get Users for
    * @return           Sequence of users
    */
  def distinctUsersIn(projectId: Int): Future[Seq[User]] = {
    val query = for {role <- this.baseQuery if role.projectId === projectId } yield role.userId
    val promise = Promise[Seq[User]]
    run(query.distinct.result).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(userIds) => promise.success(for (userId <- userIds) yield User.withId(userId).get)
    }
    promise.future
  }

  /**
    * Sets the [[RoleType]] of the specified [[ProjectRole]].
    *
    * @param role     Role to update
    * @param roleType Role type to set
    */
  def setRoleType(role: ProjectRole, roleType: RoleType)
  = run((for { model <- this.baseQuery if model.id === role.id.get } yield model.roleType).update(roleType))

}
