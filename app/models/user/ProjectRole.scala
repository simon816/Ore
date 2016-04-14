package models.user

import java.sql.Timestamp

import db.orm.model.Model
import ore.permission.role.Role
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.{Scope, ProjectScope}

case class ProjectRole(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       override val roleType: RoleType,
                       val          projectId: Int)
                       extends      Model
                       with         Role {

  override val scope: Scope = ProjectScope(this.projectId)

  def this(userId: Int, roleType: RoleType, projectId: Int) = {
    this(id=None, createdAt=None, userId=userId, roleType=roleType, projectId=projectId)
  }

}
