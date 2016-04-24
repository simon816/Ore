package controllers

import controllers.Requests.ProjectRequest
import models.project.Project
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.DataUtils
import util.forums.SpongeForums

import scala.concurrent.Future

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit ws: WSClient) extends Controller with I18nSupport with Secured {

  SpongeForums.apply
  DataUtils.apply

  protected[controllers] def withProject(author: String, slug: String)(f: Project => Result)
                                        (implicit request: RequestHeader): Result = {
    Project.withSlug(author, slug) match {
      case None => NotFound
      case Some(project) => f(project)
    }
  }

  private def projectAction(author: String, slug: String)
    = new ActionRefiner[Request, ProjectRequest] {
      def refine[A](request: Request[A]) = Future.successful {
        Project.withSlug(author, slug).map { project =>
          new ProjectRequest(project, request)
        } toRight {
          NotFound
        }
      }
  }

  /** Action to retrieve a [[Project]] and add it to the request. */
  object ProjectAction {
    def apply(author: String, slug: String) = Action andThen projectAction(author, slug)
  }

}
