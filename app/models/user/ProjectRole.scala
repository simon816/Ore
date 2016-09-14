package models.user

import java.sql.Timestamp

import db.impl.ModelKeys._
import db.impl.OreModel
import db.meta.Bind
import ore.permission.role.Role
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ProjectScope

import scala.annotation.meta.field

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
                       override val projectId: Int,
                       @(Bind @field) private var  _roleType: RoleType,
                       @(Bind @field) private var _isAccepted: Boolean = false)
                       extends OreModel(id, createdAt)
                         with Role
                         with ProjectScope
                         with Ordered[ProjectRole] {

  def this(userId: Int, roleType: RoleType, projectId: Int, accepted: Boolean) = {
    this(id=None, createdAt=None, userId=userId, _roleType=roleType, projectId=projectId, _isAccepted=accepted)
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

  /**
    * Sets whether this role has been accepted by the [[User]] it belongs to.
    *
    * @param accepted True if role accepted
    */
  def setAccepted(accepted: Boolean) = Defined {
    this._isAccepted = accepted
    update(IsAccepted)
  }

  /**
    * Returns true if this role has been accepted by the [[User]] it belongs to.
    *
    * @return True if accepted by user
    */
  def isAccepted: Boolean = this._isAccepted

  override def roleType = this._roleType
  override def compare(that: ProjectRole) = this.roleType.trust compare that.roleType.trust
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
