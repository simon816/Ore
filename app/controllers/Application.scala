package controllers

import javax.inject.Inject

import controllers.Requests.AuthRequest
import controllers.routes.{Application => self}
import db.ModelService
import db.action.ModelFilter
import db.impl.OrePostgresDriver.api._
import db.impl.ProjectTable
import db.impl.action.ProjectActions
import forums.DiscourseApi
import models.project.{Flag, Project, Version}
import models.user.User
import ore.permission._
import ore.permission.scope.GlobalScope
import ore.project.Categories.Category
import ore.project.util.{ProjectFactory, ProjectFileManager}
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.{DataUtils, OreConfig}
import views.{html => views}

/**
  * Main entry point for application.
  */
class Application @Inject()(override val messagesApi: MessagesApi,
                            implicit val projectFactory: ProjectFactory,
                            implicit val ws: WSClient,
                            implicit val config: OreConfig,
                            implicit val fileManager: ProjectFileManager,
                            implicit override val forums: DiscourseApi,
                            implicit override val service: ModelService) extends BaseController {

  private def FlagAction = Authenticated andThen PermissionAction[AuthRequest](ReviewFlags)

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def showHome(categories: Option[String], query: Option[String], sort: Option[Int]) = Action { implicit request =>
    // Get categories and sort strategy
    var categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
    val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
    
    // Determine filter
    val actions = service.provide(classOf[ProjectActions])
    val canHideProjects = User.current.isDefined && (User.current.get can HideProjects in GlobalScope)
    var filter: ProjectTable => Rep[Boolean] = query.map { q =>
      // Search filter + visible
      var f  = actions.searchFilter(q)
      if (!canHideProjects) f = f && (_.isVisible)
      f
    }.orNull[ModelFilter[ProjectTable, Project]]
    if (filter == null && !canHideProjects) filter = _.isVisible

    val future = actions.collect(filter, categoryArray, config.projects.getInt("init-load").get, -1, s)
    val projects = service.await(future).get
    if (categoryArray != null && Categories.visible.toSet.equals(categoryArray.toSet)) categoryArray = null
    Ok(views.home(projects, Option(categoryArray), s))
  }

  /**
    * Shows the moderation queue for unreviewed versions.
    *
    * @return View of unreviewed versions.
    */
  def showQueue() = {
    (Authenticated andThen PermissionAction[AuthRequest](ReviewProjects)) { implicit request =>
      Ok(views.admin.queue(Version.notReviewed.map(v => (v.project, v))))
    }
  }

  /**
    * Shows the overview page for flags.
    *
    * @return Flag overview
    */
  def showFlags() = FlagAction { implicit request =>
    Ok(views.admin.flags(Flag.unresolved))
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
    config.checkDebug()
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
      config.checkDebug()
      DataUtils.seed(users.getOrElse(200), versions.getOrElse(0), channels.getOrElse(1))
      Redirect(self.showHome(None, None, None)).withNewSession
    }
  }

  def migrate() = (Authenticated andThen PermissionAction[AuthRequest](MigrateOre)) { implicit request =>
    DataUtils.migrate()
    Redirect(self.showHome(None, None, None))
  }

}
