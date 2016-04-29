package models.user

import java.sql.Timestamp

import db.orm.dao.TModelSet
import db.orm.model.Model
import db.orm.model.ModelKeys._
import db.query.ModelQueries
import ore.permission.role.Role
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ProjectScope

/**
  * Represents a [[ore.project.ProjectMember]]'s role in a
  * [[models.project.Project]]. A ProjectRole determines what a Member can and
  * cannot do within a [[ProjectScope]].
  *
  * @param id         Model ID
  * @param createdAt  Timestamp instant of creation
  * @param userId     ID of User this role belongs to
  * @param _roleType   Type of role
  * @param projectId  ID of project this role belongs to
  */
case class ProjectRole(override val id: Option[Int] = None,
                       override val createdAt: Option[Timestamp] = None,
                       override val userId: Int,
                       private var  _roleType: RoleType,
                       override val projectId: Int)
                       extends      Model
                       with         Role
                       with         ProjectScope
                       with         Ordered[ProjectRole] { self =>

  override type M <: ProjectRole { type M = self.M }

  def this(userId: Int, roleType: RoleType, projectId: Int) = {
    this(id=None, createdAt=None, userId=userId, _roleType=roleType, projectId=projectId)
  }

  /**
    * Sets this role's [[RoleType]].
    *
    * @param _roleType Role type to set
    */
  def roleType_=(_roleType: RoleType) = {
    this._roleType = _roleType
    if (isDefined) update(RoleType)
  }

  override def roleType: RoleType = this._roleType

  override def compare(that: ProjectRole) = this.roleType.trust compare that.roleType.trust

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): ProjectRole = {
    this.copy(id = id, createdAt = theTime)
  }

  // Table bindings

  bind[RoleType](RoleType, _._roleType, roleType => Seq(ModelQueries.Users.ProjectRoles.setRoleType(this, roleType)))

}

object ProjectRole extends TModelSet[ProjectRole] {
  override def withId(id: Int): Option[ProjectRole] = ModelQueries.await(ModelQueries.Users.ProjectRoles.get(id)).get
}
