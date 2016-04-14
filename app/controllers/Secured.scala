package controllers

import controllers.Secured.Context
import models.user.{FakeUser, User}
import play.api.mvc._

/**
  * Represents a controller with user authentication / authorization.
  */
trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = {
    Results.Redirect(routes.Application.logIn(None, None, Some(request.path)))
  }

  /**
    * Ensures the client is authenticated as any User.
    *
    * @param f  Function to call if authenticated
    * @return   Result
    */
  def withAuth(f: => Context => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(Context(user))(request))
    }
  }

  /**
    * Ensures the client is authenticated and supplies a User object to work
    * with. If username is specified, this will check that the specified
    * username matches the current session.
    *
    * @param username   Username to check
    * @param f          Function to call if authenticated
    * @return           Result
    */
  def withUser(username: Option[String] = None, f: User => Request[AnyContent] => Result) = withAuth { context => implicit request =>
    if (username.isDefined && !username.get.equals(context.username)) {
      onUnauthorized(request)
    } else if (FakeUser.IsEnabled) {
      f(FakeUser)(request)
    } else {
      f(User.withName(context.username).get)(request)
    }
  }

}

object Secured {

  /**
    * Raw contextual data associated with authentication.
    *
    * @param username User username
    */
  case class Context(username: String)

}
