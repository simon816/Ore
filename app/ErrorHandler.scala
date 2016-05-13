import javax.inject._

import _root_.db.ModelService
import _root_.util.OreConfig
import _root_.forums.DiscourseApi
import play.api._
import _root_.db.impl.service.{UserBase, ProjectBase}
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

/** A custom server error handler */
class ErrorHandler @Inject()(env: Environment,
                             conf: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router],
                             implicit val config: OreConfig,
                             implicit val service: ModelService,
                             implicit val forums: DiscourseApi,
                             override val messagesApi: MessagesApi)
                             extends DefaultHttpErrorHandler(env, conf, sourceMapper, router)
                               with I18nSupport {

  implicit val users: UserBase = service.access(classOf[UserBase])
  implicit val projects: ProjectBase = service.access(classOf[ProjectBase])

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    implicit val req = request
    implicit val session = request.session
    Future.successful(InternalServerError(views.html.error(exception.getMessage)))
  }

}
