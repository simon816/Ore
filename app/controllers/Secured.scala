package controllers

import controllers.Secured.Context
import db.Storage
import models.auth.User
import play.api.mvc._

import scala.util.{Success, Failure}

trait Secured {

  def username(request: RequestHeader) = request.session.get(Security.username)

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.logIn(None, None))

  def withAuth(f: => Context => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(Context(user))(request))
    }
  }

  def withUser(f: User => Request[AnyContent] => Result) = withAuth { context => implicit request =>
    Storage.now(Storage.getUser(context.username)) match {
      case Failure(thrown) => throw thrown
      case Success(user) => f(user)(request)
    }
  }

}

object Secured {

  case class Context(username: String)

}