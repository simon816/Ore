package form

import models.user.role.OrganizationRole
import ore.permission.role.RoleTypes.RoleType

/**
  * Builds a set of [[OrganizationRole]]s from input data.
  *
  * @param users User IDs
  * @param roles Role names
  */
case class OrganizationRoleSetBuilder(name: String,
                                      override val users: List[Int],
                                      override val roles: List[String])
                                      extends RoleSetBuilder[OrganizationRole] {

  override def newRole(userId: Int, role: RoleType): OrganizationRole = new OrganizationRole(userId, role)

}
