package ore.user

import db.impl.access.UserBase
import models.user.User
import models.user.role.RoleModel
import ore.permission.scope.ScopeSubject

/**
  * Represents a [[User]] member of some entity.
  */
abstract class Member[RoleType <: RoleModel](override val userId: Int)(implicit users: UserBase)
                                             extends ScopeSubject with UserOwned {

  /**
    * Returns the [[RoleModel]]s the user has in this entity.
    *
    * @return Roles user has
    */
  def roles: Set[RoleType]

  /**
    * Returns the Member's top role.
    *
    * @return Top role
    */
  def headRole: RoleType = this.roles.toList.sorted.last

  override def user(implicit users: UserBase = null) = super.user(this.users)

}

object Member {

  implicit def toUser(member: Member[_]): User = member.user
  implicit def ordering[A <: Member[_ <: RoleModel]]: Ordering[A]
  = Ordering.by(m => (m.headRole, m.username))

}
