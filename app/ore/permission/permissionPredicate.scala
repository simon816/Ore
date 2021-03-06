package ore.permission

import db.{Model, ModelService}
import models.user.User
import models.user.role.DbRole
import ore.permission.role.{Role, Trust}
import ore.permission.scope.HasScope

import cats.Parallel
import cats.effect.{ContextShift, IO}
import cats.syntax.all._

/**
  * Permission wrapper used for chaining permission checks.
  *
  * @param user User to check
  */
class PermissionPredicate(private val user: Model[User]) extends AnyVal {

  def apply(p: Permission): AndThen = AndThen(user, p)

  def asMap(trust: Trust, globalRoles: Set[Model[DbRole]])(perms: Permission*): Map[Permission, Boolean] =
    perms.map(p => p -> apply(p).withTrustAndGlobalRoles(trust, globalRoles)).toMap

  def asMap(trust: Option[Trust], globalRoles: Option[Set[Model[DbRole]]])(
      perms: Permission*
  ): Map[Permission, Boolean] =
    perms.map(p => p -> apply(p).withTrustAndGlobalRoles(trust, globalRoles)).toMap
}

protected case class AndThen(user: Model[User], p: Permission) {
  def withTrustAndGlobalRoles(trust: Trust, globalRoles: Set[Model[DbRole]]): Boolean =
    // Special Ore Developer Case
    if (globalRoles
          .map(_.toRole)
          .contains(Role.OreDev) && (p == ViewHealth || p == ViewLogs || p == ViewActivity || p == ViewStats))
      true
    else p.trust <= trust

  def withTrustAndGlobalRoles(optTrust: Option[Trust], optGlobalRoles: Option[Set[Model[DbRole]]]): Boolean =
    (optTrust, optGlobalRoles) match {
      case (Some(trust), Some(globalRoles)) => withTrustAndGlobalRoles(trust, globalRoles)
      case _                                => false
    }

  def in[A: HasScope](subject: A)(implicit service: ModelService, cs: ContextShift[IO]): IO[Boolean] =
    Parallel.parMap2(user.trustIn(subject), user.globalRoles.allFromParent)(
      (t, r) => withTrustAndGlobalRoles(t, r.toSet)
    )

  def in[A: HasScope](subject: Option[A])(implicit service: ModelService, cs: ContextShift[IO]): IO[Boolean] =
    subject match {
      case None    => IO.pure(false)
      case Some(s) => this.in(s)
    }
}
