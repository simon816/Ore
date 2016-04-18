package models.user

import java.sql.Timestamp

import db.orm.dao.ModelDAO
import db.orm.model.Model
import db.query.Queries
import ore.permission.role.Role
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.{ProjectScope, Scope}

case class ProjectRole(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       override val roleType: RoleType,
                       override val projectId: Int)
                       extends      Model
                       with         Role
                       with         ProjectScope {

  def this(userId: Int, roleType: RoleType, projectId: Int) = {
    this(id=None, createdAt=None, userId=userId, roleType=roleType, projectId=projectId)
  }

}

object ProjectRole extends ModelDAO[ProjectRole] {
  override def withId(id: Int): Option[ProjectRole] = Queries.now(Queries.Users.ProjectRoles.get(id)).get
}
