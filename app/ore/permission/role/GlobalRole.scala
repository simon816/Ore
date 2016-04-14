package ore.permission.role

import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.{GlobalScope, Scope}

case class GlobalRole(override val userId: Int, override val roleType: RoleType) extends Role {
  override val scope: Scope = GlobalScope
}
