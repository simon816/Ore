package controllers

import java.sql.Timestamp
import java.time.Instant
import javax.inject.Inject

import controllers.sugar.Bakery
import controllers.sugar.Requests.AuthRequest
import db.impl.OrePostgresDriver.api._
import db.impl.schema.ProjectSchema
import db.{ModelFilter, ModelService}
import models.admin.Review
import models.project.{Flag, Project, Version}
import ore.Platforms.Platform
import ore.permission._
import ore.permission.scope.GlobalScope
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import ore.{OreConfig, OreEnv, Platforms}
import play.api.i18n.MessagesApi
import play.api.mvc._
import security.spauth.SingleSignOnConsumer
import util.DataHelper
import views.{html => views}

/**
  * Main entry point for application.
  */
final class Application @Inject()(data: DataHelper,
                                  implicit override val bakery: Bakery,
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
      val ordering = sort.flatMap(ProjectSortingStrategies.withId).getOrElse(ProjectSortingStrategies.Default)
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

      val validFilter = ModelFilter[Project](_.recommendedVersionId =!= -1)
      val filter = visibleFilter +&& platformFilter +&& categoryFilter +&& searchFilter +&& validFilter

      // Get projects
      val pageSize = this.config.projects.getInt("init-load").get
      val p = page.getOrElse(1)
      val offset = (p - 1) * pageSize
      val future = actions.collect(filter.fn, ordering, pageSize, offset)
      val projects = this.service.await(future).get

      if (categoryArray != null && Categories.visible.toSet.equals(categoryArray.toSet))
        categoryArray = null

      Ok(views.home(projects, Option(categoryArray), query.find(_.nonEmpty), p, ordering, pform))
    }
  }

  /**
    * Show external link warning page.
    *
    * @return External link page
    */
  def linkOut(remoteUrl: String) = {
    Action { implicit request =>
      Ok(views.linkout(remoteUrl))
    }
  }

  /**
    * Shows the moderation queue for unreviewed versions.
    *
    * @return View of unreviewed versions.
    */
  def showQueue() = {
    (Authenticated andThen PermissionAction[AuthRequest](ReviewProjects)) { implicit request =>
      Ok(views.users.admin.queue(
        this.service.access[Version](classOf[Version])
          .filterNot(_.isReviewed)
          .filterNot(_.channel.isNonReviewed)
          .filterNot(_.reviewEntries.isEmpty)
          .map(v => (v.project, v)),
        this.service.access[Version](classOf[Version])
          .filterNot(_.isReviewed)
          .filterNot(_.channel.isNonReviewed)
          .filter(_.reviewEntries.isEmpty)
          .map(v => (v.project, v))))
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
        notFound
      case Some(flag) =>
        flag.setResolved(resolved, users.current)
        Ok
    }
  }

  def showHealth() = (Authenticated andThen PermissionAction[AuthRequest](ViewHealth)) { implicit request =>
    Ok(views.users.admin.health())
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

  /**
    * Show the activities page for a user
    */
  def showActivities(user: String) = (Authenticated andThen PermissionAction[AuthRequest](ReviewProjects)) { implicit request =>
    val u = this.users.withName(user).get
    var activities: Seq[(Object, Option[Project])] = Seq.empty
    if (u.id.isDefined) {
      val reviews: Seq[(Review, Option[Project])] = this.service.access[Review](classOf[Review])
        .filter(_.userId === u.id.get)
        .take(20)
        .map(review => review -> this.projects.find(_.id === this.service.access[Version](classOf[Version]).filter(_.id === review.versionId).head.projectId))
      val flags: Seq[(Flag, Option[Project])] = this.service.access[Flag](classOf[Flag])
        .filter(_.resolvedBy === u.id.get)
        .take(20)
        .map(flag => flag -> this.projects.find(_.id === flag.projectId))
      activities = reviews ++ flags
      activities.sortWith(sortActivities)
    }
    Ok(views.users.admin.activity(u, activities))
  }

  /**
    * Compares 2 activities (supply a [[Review]] or [[Flag]]) to decide which came first
    * @param o1 Review / Flag
    * @param o2 Review / Flag
    * @return Boolean
    */
  def sortActivities(o1: Object, o2: Object): Boolean = {
    var o1Time: Long = 0
    var o2Time: Long = 0
    if (o1.isInstanceOf[Review]) {
      o1Time = o1.asInstanceOf[Review].endedAt.getOrElse(Timestamp.from(Instant.EPOCH)).getTime
    }
    if (o2.isInstanceOf[Flag]) {
      o2Time = o2.asInstanceOf[Flag].resolvedAt.getOrElse(Timestamp.from(Instant.EPOCH)).getTime
    }
    return o1Time > o2Time
  }
}
