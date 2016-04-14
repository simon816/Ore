package db.query.user

import java.sql.Timestamp

import db.UserProjectRolesTable
import db.query.Queries
import models.user.ProjectRole
import slick.lifted.TableQuery

class ProjectRolesQueries extends Queries[UserProjectRolesTable, ProjectRole](TableQuery[UserProjectRolesTable]) {

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], model: ProjectRole): ProjectRole = {
    model.copy(id = id, createdAt = theTime)
  }

}
