package ore.permission.role

import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.ScopeSubject

/**
  * Represents a "role" that is posessed by a [[models.user.User]].
  */
trait Role extends ScopeSubject {

  /**
    * ID of [[models.user.User]] this Role belongs to.
    *
    * @return User ID
    */
  def userId: Int

  /**
    * Type of role
    *
    * @return Type of role
    */
  def roleType: RoleType

}
