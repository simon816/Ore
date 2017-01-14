package controllers

import javax.inject.Inject

import controllers.Requests.AuthRequest
import db.impl.schema.ProjectSchema
import db.{ModelFilter, ModelService}
import models.project.{Flag, Project, Version}
import ore.Platforms.Platform
import ore.permission._
import ore.permission.scope.GlobalScope
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import ore.{OreConfig, OreEnv, Platforms}
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import play.api.mvc._
import util.DataHelper
import views.{html => views}

/**
  * Main entry point for application.
  */
final class Application @Inject()(data: DataHelper,
                                  implicit override val sso: SingleSignOnConsumer,
                                  implicit override val messagesApi: MessagesApi,
                                  implicit override val env: OreEnv,
                                  implicit override val config: OreConfig,
                                  implicit override val service: ModelService)
                                  extends BaseController {

  private def FlagAction = Authenticated andThen PermissionAction[AuthRequest](ReviewFlags)

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def showHome(categories: Option[String],
               query: Option[String],
               sort: Option[Int],
               page: Option[Int],
               platform: Option[String]) = {
    Action { implicit request =>
      // Get categories and sorting strategy
      val ordering = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
      val actions = this.service.getSchema(classOf[ProjectSchema])

      val canHideProjects = this.users.current.isDefined && (this.users.current.get can HideProjects in GlobalScope)
      val visibleFilter: ModelFilter[Project] = if (!canHideProjects)
        ModelFilter[Project](_.isVisible)
      else
        ModelFilter.Empty

      val pform = platform.flatMap(p => Platforms.values.find(_.name.equalsIgnoreCase(p)).map(_.asInstanceOf[Platform]))
      val platformFilter = pform.map(actions.platformFilter).getOrElse(ModelFilter.Empty)

      var categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
      val categoryFilter: ModelFilter[Project] = if (categoryArray != null)
        actions.categoryFilter(categoryArray)
      else
        ModelFilter.Empty

      val searchFilter: ModelFilter[Project] = query.map(actions.searchFilter).getOrElse(ModelFilter.Empty)

      val filter = visibleFilter +&& platformFilter +&& categoryFilter +&& searchFilter

      // Get projects
      val pageSize = this.config.projects.getInt("init-load").get
      val p = page.getOrElse(1)
      val offset = (p - 1) * pageSize
      val future = actions.collect(filter.fn, ordering, pageSize, offset)
      val models = this.service.await(future).get
      val total = this.service.await(this.service.count[Project](classOf[Project], filter.fn)).get

      if (categoryArray != null && Categories.visible.toSet.equals(categoryArray.toSet))
        categoryArray = null

      Ok(views.home(models, Option(categoryArray), query.find(_.nonEmpty), p, total, ordering, pform))
    }
  }

  /**
    * Shows the moderation queue for unreviewed versions.
    *
    * @return View of unreviewed versions.
    */
  def showQueue(page: Option[Int]) = {
    (Authenticated andThen PermissionAction[AuthRequest](ReviewProjects)) { implicit request =>
      val pageSize = this.config.admin.getInt("queue.pageSize").get
      val p = page.getOrElse(1)
      val offset = (p - 1) * pageSize
      val queue = this.projects.filter(_.needsReview, pageSize, offset)
      val total = this.service.await(this.service.count[Project](classOf[Project], _.needsReview)).get
      Ok(views.users.admin.queue(queue, p, pageSize, total))
    }
  }

  /**
    * Shows the overview page for flags.
    *
    * @return Flag overview
    */
  def showFlags() = FlagAction { implicit request =>
    val flags = this.service.access[Flag](classOf[Flag]).filterNot(_.isResolved)
    Ok(views.users.admin.flags(flags))
  }

  /**
    * Sets the resolved state of the specified flag.
    *
    * @param flagId   Flag to set
    * @param resolved Resolved state
    * @return         Ok
    */
  def setFlagResolved(flagId: Int, resolved: Boolean) = FlagAction { implicit request =>
    this.service.access[Flag](classOf[Flag]).get(flagId) match {
      case None =>
        NotFound
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
  def removeTrail(path: String) = Action(MovedPermanently('/' + path))

  /**
    * Helper route to reset Ore.
    */
  def reset() = (Authenticated andThen PermissionAction[AuthRequest](ResetOre)) { implicit request =>
    this.config.checkDebug()
    this.data.reset()
    Redirect(ShowHome).withNewSession
  }

  /**
    * Fills Ore with some dummy data.
    *
    * @return Redirect home
    */
  def seed(users: Int, projects: Int, versions: Int, channels: Int) = {
    (Authenticated andThen PermissionAction[AuthRequest](SeedOre)) { implicit request =>
      this.config.checkDebug()
      this.data.seed(users, projects, versions, channels)
      Redirect(ShowHome).withNewSession
    }
  }

  /**
    * Performs miscellaneous migration actions for use in deployment.
    *
    * @return Redirect home
    */
  def migrate() = (Authenticated andThen PermissionAction[AuthRequest](MigrateOre)) { implicit request =>
    this.data.migrate()
    Redirect(ShowHome)
  }

}
