package util

import javax.inject.Inject

import db.ModelService
import db.impl.access.UserBase
import models.user.User
import ore.OreConfig
import org.spongepowered.play.mail.Email
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request

final class EmailFactory @Inject()(override val messagesApi: MessagesApi,
                                   implicit val config: OreConfig,
                                   implicit val service: ModelService) extends I18nSupport {

  val PgpUpdated = "email.pgpUpdate"
  val AccountUnlocked = "email.accountUnlock"

  implicit val users = this.service.getModelBase(classOf[UserBase])

  def create(user: User, id: String)(implicit request: Request[_]): Email = Email(
    recipient = user.email.get,
    subject = this.messagesApi(s"$id.subject"),
    content = views.html.utils.email(
      title = s"$id.subject",
      recipient = user.name,
      body = s"$id.body"
    )
  )

}
