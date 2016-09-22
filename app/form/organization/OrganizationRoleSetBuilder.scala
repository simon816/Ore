package form.organization

import models.user.role.OrganizationRole

/**
  * Builds a set of [[OrganizationRole]]s from input data.
  *
  * @param users User IDs
  * @param roles Role names
  */
case class OrganizationRoleSetBuilder(name: String,
                                      override val users: List[Int],
                                      override val roles: List[String])
                                      extends TOrganizationRoleSetBuilder
