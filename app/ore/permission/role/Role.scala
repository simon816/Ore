package ore.permission.role

import db.orm.model.UserOwner
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ScopeSubject

/**
  * Represents a "role" that is posessed by a [[models.user.User]].
  */
trait Role extends ScopeSubject with UserOwner {
  /** Type of role */
  def roleType: RoleType
}
