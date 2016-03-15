package controllers

import javax.inject.Inject

import auth.DiscourseSSO._
import db.Storage
import models.auth.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import views.{html => views}

import scala.util.{Failure, Success}

class Application @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action { implicit request =>
    Storage.now(Storage.getProjects) match {
      case Failure(thrown) => throw thrown
      case Success(projects) => Ok(views.index(projects))
    }
  }

  def logIn(sso: Option[String], sig: Option[String]) = Action {
    if (sso.isEmpty || sig.isEmpty) {
      Redirect(getRedirect)
    } else {
      val userData = authenticate(sso.get, sig.get)
      var user = new User(userData._1, userData._2, userData._3, userData._4)
      user = Storage.findOrCreate(user)
      Redirect(routes.Application.index()).withSession(Security.username -> user.username, "email" -> user.email)
    }
  }

  def logOut = Action { implicit request =>
    Redirect(routes.Application.index()).withNewSession
  }

}
