package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Projects => self}
import controllers.routes.{Application => app}
import models.project._
import models.user.ProjectRole
import ore.permission.EditSettings
import ore.permission.role.RoleTypes
import ore.project.{Categories, InvalidPluginFileException, ProjectManager}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.Forms
import util.Input._
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * Controller for handling Project related actions.
  *
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi, ws: WSClient) extends BaseController(ws) {

  private def SettingsEditAction(author: String, slug: String) = {
    AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditSettings)
  }

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreator = Authenticated { implicit request =>
    Ok(views.projects.create(None))
  }

  /**
    * Uploads a Project's first plugin file for further processing.
    *
    * @return Result
    */
  def upload = Authenticated { implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreator()).flashing("error" -> "No file submitted.")
      case Some(tmpFile) =>
        // Initialize plugin file
        val user = request.user
        ProjectManager.initUpload(tmpFile.ref, tmpFile.filename, user) match {
          case Failure(thrown) => if (thrown.isInstanceOf[InvalidPluginFileException]) {
            // PEBKAC
            Redirect(self.showCreator()).flashing("error" -> "Invalid plugin file.")
          } else {
            throw thrown
          }
          case Success(plugin) =>
            // Cache pending project for later use
            val meta = plugin.meta.get
            val project = Project.fromMeta(user, meta)
            Project.setPending(project, plugin)
            Redirect(self.showCreatorWithMeta(project.ownerName, project.slug))
        }
    }
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author Author of plugin
    * @param slug   Project slug
    * @return Create project view
    */
  def showCreatorWithMeta(author: String, slug: String) = Authenticated { implicit request =>
    Project.getPending(author, slug) match {
      case None => Redirect(self.showCreator())
      case Some(pending) =>
        pending.initFirstVersion
        Ok(views.projects.create(Some(pending)))
    }
  }

  /**
    * Shows the members configuration page during Project creation.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of members config
    */
  def showMembersConfig(author: String, slug: String) = Authenticated { implicit request =>
    Project.getPending(author, slug) match {
      case None => BadRequest("No pending project")
      case Some(project) =>
        val form = Forms.ProjectSave.bindFromRequest.get
        project.project.category = Categories.withName(form._1.trim)
        project.project.issues = nullIfEmpty(form._2)
        project.project.source = nullIfEmpty(form._3)
        project.project.description = nullIfEmpty(form._4)
        Ok(views.projects.membersConfig(project))
    }
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author Author of project
    * @param slug   Project slug
    * @return Redirection to project page if successful
    */
  def showFirstVersionCreator(author: String, slug: String) = Authenticated { implicit request =>
    Project.getPending(author, slug) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val form = Forms.MemberRoles.bindFromRequest.get
        val roleNames = form._2
        val roles = for ((userId, i) <- form._1.zipWithIndex) yield {
          new ProjectRole(userId, RoleTypes.withName(roleNames(i)), -1)
        }
        pendingProject.roles = roles.toSet

        val pendingVersion = pendingProject.pendingVersion.get
        Redirect(routes.Versions.showCreatorWithMeta(
          author, slug, pendingVersion.channelName, pendingVersion.version.versionString))
    }
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return View of project
    */
  def show(author: String, slug: String) = {
    ProjectAction(author, slug, countView = true) { implicit request =>
      val project = request.project
      Ok(views.projects.pages.view(project, project.homePage))
    }
  }

  /**
    * Shortcut for navigating to a project.
    *
    * @param pluginId Project pluginId
    * @return Redirect to project page.
    */
  def shortcut(pluginId: String) = Action { implicit request =>
    Project.withPluginId(pluginId) match {
      case None => NotFound
      case Some(project) => Redirect(self.show(project.ownerName, project.slug))
    }
  }

  /**
    * Saves the specified Project from the settings manager.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return View of project
    */
  def save(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      val project = request.project
      val form = Forms.ProjectSave.bindFromRequest.get
      project.category = Categories.withName(form._1.trim)
      project.issues = nullIfEmpty(form._2)
      project.source = nullIfEmpty(form._3)
      project.description = nullIfEmpty(form._4)
      Redirect(self.show(author, slug))
    }
  }

  /**
    * Sets the "starred" status of a Project for the current user.
    *
    * @param author  Project owner
    * @param slug    Project slug
    * @param starred True if should set to starred
    * @return Result code
    */
  def setStarred(author: String, slug: String, starred: Boolean) = {
    AuthedProjectAction(author, slug) { implicit request =>
      val project = request.project
      val user = request.user
      val alreadyStarred = project.isStarredBy(user)
      if (starred) {
        if (!alreadyStarred) {
          project.starFor(user)
        }
      } else if (alreadyStarred) {
        project.unstarFor(user)
      }
      Ok
    }
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return View of project
    */
  def showDiscussion(author: String, slug: String) = {
    ProjectAction(author, slug, countView = true) { implicit request =>
      Ok(views.projects.discuss(request.project))
    }
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Project manager
    */
  def showManager(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      Ok(views.projects.manage(request.project))
    }
  }

  /**
    * Redirect's to the project's issue tracker if any.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Issue tracker
    */
  def showIssues(author: String, slug: String) = {
    ProjectAction(author, slug) { implicit request =>
      request.project.issues match {
        case None => NotFound
        case Some(link) => Redirect(link)
      }
    }
  }

  /**
    * Redirect's to the project's source code if any.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Source code
    */
  def showSource(author: String, slug: String) = {
    ProjectAction(author, slug) { implicit request =>
      request.project.source match {
        case None => NotFound
        case Some(link) => Redirect(link)
      }
    }
  }

  /**
    * Renames the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Project homepage
    */
  def rename(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      val newName = compact(Forms.ProjectRename.bindFromRequest.get)
      if (!Project.isNamespaceAvailable(author, slugify(newName))) {
        Redirect(self.showManager(author, slug)).flashing("error" -> "That name is not available.")
      } else {
        val project = request.project
        project.name = newName
        Redirect(self.show(author, project.slug))
      }
    }
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Home page
    */
  def delete(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      val project = request.project
      project.delete.get
      Redirect(app.showHome(None))
        .flashing("success" -> ("Project \"" + project.name + "\" deleted."))
    }
  }

}
