package controllers

import models.project.Project
import models.user.User
import ore.Statistics
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
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

  /**
    * A request that holds a [[Project]].
    *
    * @param project Project to hold
    * @param request Request to wrap
    */
  class ProjectRequest[A](val project: Project, request: Request[A]) extends WrappedRequest[A](request)

  private def projectAction(author: String, slug: String, countView: Boolean = false)
    = new ActionRefiner[Request, ProjectRequest] {
      def refine[A](request: Request[A]) = Future.successful {
        Project.withSlug(author, slug).map { project =>
          if (countView) Statistics.projectViewed(project)(request)
          new ProjectRequest(project, request)
        } toRight {
          NotFound
        }
      }
  }

  /** Action to retrieve a [[Project]] and add it to the request. */
  object ProjectAction {
    def apply(author: String, slug: String, countView: Boolean = false) = {
      Action andThen projectAction(author, slug, countView)
    }
  }

}
