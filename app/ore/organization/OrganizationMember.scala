package ore.organization

import db.impl.access.UserBase
import models.user.Organization
import models.user.role.OrganizationRole
import ore.permission.scope.Scope
import ore.user.Member

/**
  * Represents a [[models.user.User]] member of an [[Organization]].
  *
  * @param organization Organization member belongs to
  * @param userId       User ID
  * @param users        UserBase instance
  */
class OrganizationMember(val organization: Organization, override val userId: Int)(implicit users: UserBase)
                         extends Member[OrganizationRole](userId) {

  override def roles: Set[OrganizationRole] = this.organization.memberships.getRoles(this.user)
  override def scope: Scope = this.organization.scope

}

object OrganizationMember {

  implicit def ordering[A <: OrganizationMember] = Member.ordering[A]

}
