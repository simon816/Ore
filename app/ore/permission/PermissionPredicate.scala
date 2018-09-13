package ore.permission

import models.user.User
import ore.permission.role.{RoleType, Trust}
import ore.permission.scope.ScopeSubject
import scala.concurrent.{ExecutionContext, Future}

import db.ModelService

/**
  * Permission wrapper used for chaining permission checks.
  *
  * @param user User to check
  */
case class PermissionPredicate(user: User) {

  def apply(p: Permission)(implicit ec: ExecutionContext): AndThen = AndThen(user, p)

  protected case class AndThen(user: User, p: Permission)(implicit ec: ExecutionContext) {
    def withTrust(trust: Trust): Boolean = {
      // Special Ore Developer Case
      if (user.globalRoles.contains(RoleType.OreDev) && (p == ViewHealth || p == ViewLogs || p == ViewActivity || p == ViewStats)) true
      else p.trust <= trust
    }

    def in(subject: ScopeSubject)(implicit service: ModelService): Future[Boolean] = user.trustIn(subject.scope).map(withTrust)

    def in(subject: Option[ScopeSubject])(implicit service: ModelService): Future[Boolean] = {
      subject match {
        case None => Future.successful(false)
        case Some(s) => this.in(s)
      }
    }
  }
}
