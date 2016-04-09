package controllers

import javax.inject.Inject

import auth.DiscourseSSO._
import controllers.routes.{Application => self}
import db.query.Queries
import db.query.Queries.now
import models.auth.{FakeUser, User}
import models.project.Categories.Category
import models.project.{Categories, Project}
import play.api.i18n.MessagesApi
import play.api.mvc._
import views.{html => views}

import scala.concurrent.Future

/**
  * Main entry point for application.
  */
class Application @Inject()(override val messagesApi: MessagesApi) extends BaseController {

  /**
    * The maximum amount of Projects that are loaded initially.
    */
  val INITIAL_PROJECT_LOAD: Int = 50

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def showHome(categories: Option[String]) = Action { implicit request =>
    var projectsFuture: Future[Seq[Project]] = null
    var categoryArray: Array[Category] = null
    categories match {
      case None => projectsFuture = Queries.Projects.collect(limit = INITIAL_PROJECT_LOAD)
      case Some(csv) =>
        categoryArray = Categories.fromString(csv)
        var categoryIds = categoryArray.map(_.id)
        if (Categories.values.subsetOf(categoryArray.toSet) || categoryArray.isEmpty) {
          categoryArray = null
          categoryIds = null
        }
        projectsFuture = Queries.Projects.collect(categoryIds, INITIAL_PROJECT_LOAD)
    }
    val projects = Queries.now(projectsFuture).get
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
      now(Queries.Users.getOrCreate(FakeUser))
      Redirect(self.showHome(None)).withSession(Security.username -> FakeUser.username, "email" -> FakeUser.email)
    } else if (sso.isEmpty || sig.isEmpty) {
      Redirect(getRedirect)
    } else {
      val userData = authenticate(sso.get, sig.get)
      var user = new User(userData._1, userData._2, userData._3, userData._4)
      user = now(Queries.Users.getOrCreate(user)).get
      Redirect(self.showHome(None)).withSession(Security.username -> user.username, "email" -> user.email)
    }
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut = Action { implicit request =>
    Redirect(self.showHome(None)).withNewSession
  }

}
