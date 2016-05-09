import javax.inject._

import play.api._
import _root_.db.ModelService
import forums.DiscourseApi
import play.api.http.DefaultHttpErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router

import scala.concurrent._

class ErrorHandler @Inject()(env: Environment,
                             config: Configuration,
                             sourceMapper: OptionalSourceMapper,
                             router: Provider[Router],
                             implicit val models: ModelService,
                             implicit val forums: DiscourseApi,
                             override val messagesApi: MessagesApi)
                             extends DefaultHttpErrorHandler(env, config, sourceMapper, router)
                             with I18nSupport {

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    implicit val req = request
    implicit val session = request.session
    Future.successful(InternalServerError(views.html.error(exception.getMessage)))
  }

}
