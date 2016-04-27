package controllers

import javax.inject.Inject

import controllers.Requests.AuthRequest
import controllers.routes.{Application => self}
import db.OrePostgresDriver.api._
import db.ProjectTable
import db.query.Queries
import db.query.Queries.{ModelFilter, filterToFunction, now}
import models.project.Project._
import models.project.{Flag, Project}
import models.user.User
import ore.permission.scope.GlobalScope
import ore.permission.{HideProjects, ResetOre, ReviewFlags, SeedOre}
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.C._
import util.DataUtils
import views.{html => views}

/**
  * Main entry point for application.
  */
class Application @Inject()(override val messagesApi: MessagesApi, implicit val ws: WSClient) extends BaseController {

  private def FlagAction = Authenticated andThen PermissionAction[AuthRequest](ReviewFlags)

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def showHome(categories: Option[String], query: Option[String], sort: Option[Int]) = Action { implicit request =>
    // Get categories and sort strategy
    val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
    val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)

    // Determine filter
    val canHideProjects = User.current.isDefined && (User.current.get can HideProjects in GlobalScope)
    var filter: ProjectTable => Rep[Boolean] = query.map { q =>
      // Search filter + visible
      var f  = Queries.Projects.searchFilter(q)
      if (!canHideProjects) f = f && (_.isVisible)
      f
    }.orNull[ModelFilter[ProjectTable, Project]]
    if (filter == null && !canHideProjects) filter = _.isVisible

    val projects = now(Queries.Projects.collect(filter, categoryArray, InitialLoad, -1, s)).get
    Ok(views.home(projects, Option(categoryArray), s))
  }

  /**
    * Shows the overview page for flags.
    *
    * @return Flag overview
    */
  def showFlags() = FlagAction { implicit request =>
    Ok(views.flags(Flag.unresolved))
  }

  /**
    * Sets the resolved state of the specified flag.
    *
    * @param flagId   Flag to set
    * @param resolved Resolved state
    * @return         Ok
    */
  def setFlagResolved(flagId: Int, resolved: Boolean) = FlagAction { implicit request =>
    Flag.withId(flagId) match {
      case None => NotFound
      case Some(flag) =>
        flag.setResolved(resolved)
        Ok
    }
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
    */
  def reset = (Authenticated andThen PermissionAction[AuthRequest](ResetOre)) { implicit request =>
    checkDebug()
    DataUtils.reset()
    Redirect(self.showHome(None, None, None)).withNewSession
  }

  /**
    * Fills Ore with some dummy data.
    *
    * @return Redirect home
    */
  def seed(users: Option[Int], versions: Option[Int], channels: Option[Int]) = {
    (Authenticated andThen PermissionAction[AuthRequest](SeedOre)) { implicit request =>
      checkDebug()
      DataUtils.seed(users.getOrElse(200), versions.getOrElse(0), channels.getOrElse(1))
      Redirect(self.showHome(None, None, None)).withNewSession
    }
  }

}
