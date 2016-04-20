package db.query.user

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.UserProjectRolesTable
import db.query.Queries
import db.query.Queries.DB._
import models.user.ProjectRole
import slick.lifted.TableQuery

import scala.concurrent.Future

class ProjectRolesQueries extends Queries[UserProjectRolesTable, ProjectRole](TableQuery[UserProjectRolesTable]) {

  /**
    * Returns all the user IDs that have one or more roles in the specified
    * Project.
    *
    * @param projectId  Project to get Users for
    * @return           Sequence of user IDs
    */
  def distinctUsersIn(projectId: Int): Future[Seq[Int]] = {
    val query = for { role <- this.models if role.projectId === projectId } yield role.userId
    run(query.distinct.result)
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], model: ProjectRole): ProjectRole = {
    model.copy(id = id, createdAt = theTime)
  }

}
