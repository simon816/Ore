package models.viewhelper

import scala.concurrent.{ExecutionContext, Future}

import db.ModelService
import models.user.{Organization, User}
import ore.permission._

import cats.data.OptionT
import cats.instances.future._

case class ScopedOrganizationData(permissions: Map[Permission, Boolean] = Map.empty)

object ScopedOrganizationData {

  val noScope = ScopedOrganizationData()

  def cacheKey(orga: Organization, user: User) = s"""organization${orga.id.value}foruser${user.id.value}"""

  def of[A](currentUser: Option[User], orga: Organization)(
      implicit ec: ExecutionContext,
      service: ModelService
  ): Future[ScopedOrganizationData] = {
    if (currentUser.isEmpty) Future.successful(noScope)
    else {
      for {
        editSettings <- (currentUser.get.can(EditSettings) in orga).map((EditSettings, _))
      } yield {

        val perms: Map[Permission, Boolean] = Seq(editSettings).toMap
        ScopedOrganizationData(perms)
      }
    }
  }

  def of[A](currentUser: Option[User], orga: Option[Organization])(
      implicit ec: ExecutionContext,
      service: ModelService
  ): OptionT[Future, ScopedOrganizationData] =
    OptionT.fromOption[Future](orga).semiflatMap(of(currentUser, _))
}
