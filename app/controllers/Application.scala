package controllers

import javax.inject.Inject

import auth.DiscourseSSO._
import controllers.routes.{Application => self}
import db.Storage
import models.auth.{FakeUser, User}
import models.project.Categories.Category
import models.project.{Categories, Project}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import views.{html => views}

import scala.concurrent.Future

class Application @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val INITIAL_PROJECT_LOAD: Int = 50

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index(categories: Option[String]) = Action { implicit request =>
    var projectsFuture: Future[Seq[Project]] = null
    var categoryArray: Array[Category] = null

    categories match {
      case None => projectsFuture = Storage.projects(limit = INITIAL_PROJECT_LOAD)
      case Some(csv) =>
        categoryArray = Categories.fromString(csv)
        if (Categories.values.subsetOf(categoryArray.toSet)) {
          categoryArray = null
        }
        projectsFuture = Storage.projects(categoryArray.map(_.id), INITIAL_PROJECT_LOAD)
    }

    val projects = Storage.now(projectsFuture).get
    Ok(views.index(projects, Option(categoryArray)))
  }

  /**
    * Redirect to forums for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from forums
    * @param sig  Incoming signature from forums
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String]) = Action {
    if (FakeUser.ENABLED) {
      Storage.getOrCreateUser(FakeUser)
      Redirect(self.index(None)).withSession(Security.username -> FakeUser.username, "email" -> FakeUser.email)
    } else if (sso.isEmpty || sig.isEmpty) {
      Redirect(getRedirect)
    } else {
      val userData = authenticate(sso.get, sig.get)
      var user = new User(userData._1, userData._2, userData._3, userData._4)
      user = Storage.getOrCreateUser(user)
      Redirect(self.index(None)).withSession(Security.username -> user.username, "email" -> user.email)
    }
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut = Action { implicit request =>
    Redirect(self.index(None)).withNewSession
  }

}
