package controllers

import javax.inject.Inject

import controllers.routes.{Application => self}
import db.OrePostgresDriver.api._
import db.ProjectTable
import db.query.Queries
import db.query.Queries.now
import models.project.Project._
import models.user.{FakeUser, User}
import ore.permission.{ResetOre, SeedOre}
import ore.project.{ProjectSortingStrategies, Categories}
import ore.project.Categories.Category
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.C._
import util.DataUtils
import util.form.Forms
import util.forums.SpongeForums._
import views.{html => views}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Main entry point for application.
  */
class Application @Inject()(override val messagesApi: MessagesApi, implicit val ws: WSClient) extends BaseController {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def showHome(categories: Option[String], query: Option[String], sort: Option[Int]) = Action { implicit request =>
    val categoryArray: Array[Category] = if (categories.isDefined) Categories.fromString(categories.get) else null
    val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
    val filter: ProjectTable => Rep[Boolean] = if (query.isDefined) {
      val q = '%' + query.get.toLowerCase + '%'
      p => (p.name.toLowerCase like q) || (p.description.toLowerCase like q) || (p.ownerName.toLowerCase like q)
    } else null
    val projects = now(Queries.Projects.collect(filter, categoryArray, InitialLoad, s)).get
    Ok(views.home(projects, Option(categoryArray), s))
  }

  /**
    * Shows the User page for the user with the specified username.
    *
    * @param username   Username to lookup
    * @return           View of user page
    */
  def showUser(username: String) = Action { implicit request =>
    User.withName(username) match {
      case None => NotFound
      case Some(user) => Ok(views.user(user))
    }
  }

  /**
    * Submits a change to the specified user's tagline.
    *
    * @param username   User to update
    * @return           View of user page
    */
  def saveTagline(username: String) = Authenticated { implicit request =>
    val user = request.user
    val tagline = Forms.UserTagline.bindFromRequest.get.trim
    if (tagline.length > User.MaxTaglineLength) {
      Redirect(self.showUser(user.username)).flashing("error" -> "Tagline is too long.")
    } else {
      user.tagline = tagline
      Redirect(self.showUser(user.username))
    }
  }

  /**
    * Redirect to forums for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from forums
    * @param sig  Incoming signature from forums
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String], returnPath: Option[String]) = Action { implicit request =>
    val baseUrl = AppConf.getString("baseUrl").get
    if (FakeUser.IsEnabled) {
      now(Queries.Users.getOrInsert(FakeUser))
      Redirect(self.showHome(None, None, None)).withSession(Security.username -> FakeUser.username)
    } else if (sso.isEmpty || sig.isEmpty) {
      Redirect(Auth.getRedirect(baseUrl + "/login"))
        .flashing("url" -> returnPath.getOrElse(request.path))
    } else {
      val userData = Auth.authenticate(sso.get, sig.get)
      var user = new User(userData._1, userData._2, userData._3, userData._4)
      user = now(Queries.Users.getOrInsert(user)).get

      Users.fetchRoles(user.username).andThen {
        case roles => if (!roles.equals(user.globalRoleTypes)) user.globalRoleTypes = roles.get
      }

      Redirect(baseUrl + request2flash.get("url").get).withSession(Security.username -> user.username)
    }
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut = Action { implicit request =>
    Redirect(self.showHome(None, None, None)).withNewSession
  }

  /**
    * Removes a trailing slash from a route.
    *
    * @param path Path with trailing slash
    * @return     Redirect to proper route
    */
  def removeTrail(path: String) = Action {
    MovedPermanently('/' + path)
  }

  /**
    * Helper route to reset Ore.
    *
    * TODO: REMOVE BEFORE PRODUCTION
    */
  def reset = (Authenticated andThen PermissionAction[AuthRequest](ResetOre)) { implicit request =>
    DataUtils.reset()
    Redirect(self.showHome(None, None, None)).withNewSession
  }

  /**
    * Fills Ore with some dummy data.
    *
    * @return Redirect home
    */
  def seed = (Authenticated andThen PermissionAction[AuthRequest](SeedOre)) { implicit request =>
    DataUtils.seed()
    Redirect(self.showHome(None, None, None)).withNewSession
  }

}
