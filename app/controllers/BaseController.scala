package controllers

import models.project.Project
import models.user.User
import ore.Statistics
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc._
import util.forums.SpongeForums._

import scala.concurrent.Future

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(ws: WSClient) extends Controller with I18nSupport with Secured {

  if (API == null) init(ws)

  protected[controllers] def withProject(author: String, slug: String, f: Project => Result)
                                        (implicit request: RequestHeader): Result = {
    Project.withSlug(author, slug) match {
      case None => NotFound
      case Some(project) => f(project)
    }
  }

}
