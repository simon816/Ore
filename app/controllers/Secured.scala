package controllers

import controllers.Secured.Context
import db.Storage
import models.auth.User
import play.api.mvc._

import scala.util.{Failure, Success}

/**
  * Represents a controller with user authentication / authorization.
  */
trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.logIn(None, None))

  def withAuth(f: => Context => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(Context(user))(request))
    }
  }

  def withUser(username: Option[String] = None, f: User => Request[AnyContent] => Result) = withAuth { context => implicit request =>
    if (username.isDefined && !username.get.equals(context.username)) {
      onUnauthorized(request)
    } else {
      Storage.now(Storage.getUser(context.username)) match {
        case Failure(thrown) => throw thrown
        case Success(user) => f(user)(request)
      }
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
