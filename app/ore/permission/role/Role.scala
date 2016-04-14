package ore.permission.role

import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.{GlobalScope, Scope}

trait Role {

  def userId: Int

  def roleType: RoleType

  def scope: Scope = GlobalScope

}
