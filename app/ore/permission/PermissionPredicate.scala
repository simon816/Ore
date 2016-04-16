package ore.permission

import models.user.User
import ore.permission.scope.{Scope, ScopeSubject}

/**
  * Permission wrapper used for chaining permission checks.
  *
  * @param user User to check
  */
case class PermissionPredicate(user: User) {

  def apply(p: Permission): AndThen = AndThen(user, p)

  protected case class AndThen(user: User, p: Permission) {
    def in(subject: ScopeSubject): Boolean = subject.scope.test(user, p)
  }

}
