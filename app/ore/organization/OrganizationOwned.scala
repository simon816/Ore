package ore.organization

import db.impl.access.OrganizationBase
import models.user.Organization

/**
  * Represents anything that has an [[Organization]].
  */
trait OrganizationOwned {
  /** Returns the Organization's ID */
  def organizationId: Int
  /** Returns the Organization */
  def organization(implicit organizations: OrganizationBase): Organization = organizations.get(this.organizationId).get
}
