package ore.permission

import models.user.User
import ore.permission.scope.ScopeSubject

/**
  * Permission wrapper used for chaining permission checks.
  *
  * @param user User to check
  */
case class PermissionPredicate(user: User, not: Boolean = false) {

  def apply(p: Permission): AndThen = AndThen(user, p, not)

  protected case class AndThen(user: User, p: Permission, not: Boolean) {
    def in(subject: ScopeSubject): Boolean = {
      val result = subject.scope.test(user, p)
      if (not) !result else result
    }
  }

}
