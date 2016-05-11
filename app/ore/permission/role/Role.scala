package ore.permission.role

import ore.UserOwned
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ScopeSubject

/**
  * Represents a "role" that is posessed by a [[models.user.User]].
  */
trait Role extends ScopeSubject with UserOwned {
  /** Type of role */
  def roleType: RoleType
}
