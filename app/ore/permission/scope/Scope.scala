package ore.permission.scope

import db.ModelService
import models.user.User
import ore.permission.Permission

/**
  * Represents a "scope" for testing permissions within the application.
  */
trait Scope extends ScopeSubject {

  /**
    * Returns the parent scope for this scope if any.
    *
    * @return Parent scope if any, None otherwise
    */
  def parent: Option[Scope] = None

  /**
    * Test the given permission for the given user in this scope only.
    *
    * @param user User to check on
    * @param p    Permission to test
    * @return     True if user has permission in this scope
    */
  def check(user: User, p: Permission)(implicit service: ModelService): Boolean = p.trust <= user.trustIn(this)

  /**
    * Tests the given permission for the given user.
    *
    * @param user User to check on
    * @param p    Permission to test
    * @return     True if user has permission
    */
  def test(user: User, p: Permission)(implicit service: ModelService): Boolean = {
    var result: Boolean = false
    var scope: Option[Scope] = Some(this)
    while (scope.isDefined || (scope.isDefined && !result)) {
      val s = scope.get
      result |= s.check(user, p)
      scope = s.parent
    }
    result
  }

  override val scope: Scope = this

}
