package models.viewhelper

import db.{Model, ModelService}
import db.access.ModelView
import models.project.{Flag, Project}
import models.user.{Organization, User}
import ore.permission._
import util.syntax._

import cats.effect.{ContextShift, IO}
import cats.syntax.all._

/**
  * Holds ProjectData that is specific to a user
  */
object ScopedProjectData {

  def cacheKey(project: Model[Project], user: Model[User]) = s"""project${project.id}foruser${user.id}"""

  def of(
      currentUser: Option[Model[User]],
      project: Model[Project]
  )(implicit service: ModelService, cs: ContextShift[IO]): IO[ScopedProjectData] = {
    currentUser
      .map { user =>
        (
          project.owner.user
            .flatMap(_.toMaybeOrganization(ModelView.now(Organization)).value)
            .flatMap(orgaOwner => user.can(PostAsOrganization) in orgaOwner),
          user.hasUnresolvedFlagFor(project, ModelView.now(Flag)),
          project.stars.contains(user),
          project.watchers.contains(user),
          user.trustIn(project),
          user.globalRoles.allFromParent
        ).parMapN {
          case (
              canPostAsOwnerOrga,
              uProjectFlags,
              starred,
              watching,
              projectTrust,
              globalRoles
              ) =>
            val perms   = EditPages :: EditSettings :: EditChannels :: EditVersions :: UploadVersions :: ReviewProjects :: Nil
            val permMap = user.can.asMap(projectTrust, globalRoles.toSet)(perms: _*)
            ScopedProjectData(canPostAsOwnerOrga, uProjectFlags, starred, watching, permMap)
        }
      }
      .getOrElse(IO.pure(noScope))
  }

  val noScope = ScopedProjectData()
}

case class ScopedProjectData(
    canPostAsOwnerOrga: Boolean = false,
    uProjectFlags: Boolean = false,
    starred: Boolean = false,
    watching: Boolean = false,
    permissions: Map[Permission, Boolean] = Map.empty
) {

  def perms(perm: Permission): Boolean = permissions.getOrElse(perm, false)

}
