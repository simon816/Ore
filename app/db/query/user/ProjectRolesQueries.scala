package db.query.user

import db.ProjectRoleTable
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import db.query.ModelQueries.run
import models.user.{ProjectRole, User}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

class ProjectRolesQueries extends ModelQueries {

  override type Row = ProjectRole
  override type Table = ProjectRoleTable

  override val modelClass = classOf[ProjectRole]
  override val baseQuery = TableQuery[ProjectRoleTable]

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

}
