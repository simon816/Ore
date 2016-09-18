package models.user.role

import java.sql.Timestamp

import db.Model
import db.meta.Bind
import ore.Visitable
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.OrganizationScope

import scala.annotation.meta.field

/**
  * Represents a [[RoleModel]] within an [[models.user.Organization]].
  *
  * @param id             Model ID
  * @param createdAt      Timestamp instant of creation
  * @param userId         ID of User this role belongs to
  * @param organizationId ID of Organization this role belongs to
  * @param _roleType      Type of Role
  * @param _isAccepted    True if has been accepted
  */
case class OrganizationRole(override val id: Option[Int] = None,
                            override val createdAt: Option[Timestamp] = None,
                            override val userId: Int,
                            override val organizationId: Int = -1,
                            @(Bind @field) private var _roleType: RoleType,
                            @(Bind @field) private var _isAccepted: Boolean = false)
                            extends RoleModel(id, createdAt, userId, _roleType, _isAccepted)
                              with OrganizationScope {

  def this(userId: Int, roleType: RoleType) = this(userId = userId, _roleType = roleType)

  override def subject: Visitable = this.organization
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
