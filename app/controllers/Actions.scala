package controllers

import controllers.Requests.{AuthRequest, AuthedProjectRequest, ScopedRequest}
import models.project.Project
import models.user.User
import ore.permission.scope.GlobalScope
import ore.permission.{HideProjects, Permission}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Represents a controller with user authentication / authorization.
  */
trait Actions {

  def onUnauthorized(request: RequestHeader) = {
    Redirect(routes.Application.logIn(None, None, Some(request.path)))
  }

  // Auth

  /** Action that ensures that the request is authenticated. */
  object Authenticated extends ActionBuilder[AuthRequest] with ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      User.current(request.session)
        .map(new AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  // Permissions

  private def authedProjectAction(author: String, slug: String)
    = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
      def refine[A](request: AuthRequest[A]) = Future.successful {
        Project.withSlug(author, slug) match {
          case None => Left(NotFound)
          case Some(project) =>
            if (project.isVisible || (request.user can HideProjects in GlobalScope)) {
              Right(new AuthedProjectRequest(project, request))
            } else {
              Left(NotFound)
            }
        }
      }
  }

  /** Action to retrieve a [[Project]] after authentication has been performed. */
  object AuthedProjectAction {
    def apply(author: String, slug: String) = Authenticated andThen authedProjectAction(author, slug)
  }

  /**
    * Action to perform a permission check for the current ScopedRequest and
    * given Permission.
    *
    * @param p  Permission to check
    * @tparam R Type of ScopedRequest that is being checked
    * @return   The ScopedRequest as an instance of R
    */
  def PermissionAction[R[_] <: ScopedRequest[_]](p: Permission) = new ActionRefiner[ScopedRequest, R] {
    def refine[A](request: ScopedRequest[A]) = Future.successful {
      if (!(request.user can p in request.subject)) {
        Left(onUnauthorized(request))
      } else {
        Right(request.asInstanceOf[R[A]])
      }
    }
  }

  /**
    * A PermissionAction that uses an AuthedProjectRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   An [[AuthedProjectRequest]]
    */
  def ProjectPermissionAction(p: Permission) = PermissionAction[AuthedProjectRequest](p)

}
