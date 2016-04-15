package controllers

import models.user.User
import play.api.mvc.Results._
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc._

import scala.concurrent.Future

/**
  * Represents a controller with user authentication / authorization.
  */
trait Secured {

  def onUnauthorized(request: RequestHeader) = {
    Redirect(routes.Application.logIn(None, None, Some(request.path)))
  }

  case class AuthRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

  object Authenticated extends ActionBuilder[AuthRequest] with ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      User.current(request.session)
        .map(new AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }
}
