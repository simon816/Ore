package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Projects => self}
import controllers.routes.{Application => app}
import db.ModelService
import form.Forms
import forums.DiscourseApi
import models.project._
import models.user.User
import ore.permission.{EditSettings, HideProjects}
import ore.project.FlagReasons
import ore.statistic.StatTracker
import ore.project.util.{InvalidPluginFileException, ProjectFactory}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.Conf
import util.StringUtils._
import views.html.{projects => views}

import scala.util.{Failure, Success}

/**
  * Controller for handling Project related actions.
  *
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi,
                         val stats: StatTracker,
                         implicit val projectFactory: ProjectFactory,
                         implicit val forums: DiscourseApi,
                         implicit val ws: WSClient,
                         implicit val service: ModelService) extends BaseController {

  private def SettingsEditAction(author: String, slug: String)
  = AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditSettings)

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreator = Authenticated(service, forums) { implicit request =>
    Ok(views.create(None))
  }

  /**
    * Uploads a Project's first plugin file for further processing.
    *
    * @return Result
    */
  def upload = Authenticated(service, forums) { implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreator()).flashing("error" -> "No file submitted.")
      case Some(tmpFile) =>
        // Initialize plugin file
        val user = request.user
        projectFactory.initUpload(tmpFile.ref, tmpFile.filename, user) match {
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
  def showCreatorWithMeta(author: String, slug: String) = {
    Authenticated(service, forums) { implicit request =>
      Project.getPending(author, slug) match {
        case None => Redirect(self.showCreator())
        case Some(pending) => Ok(views.create(Some(pending)))
      }
    }
  }

  /**
    * Shows the members configuration page during Project creation.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of members config
    */
  def showMembersConfig(author: String, slug: String) = {
    Authenticated(service, forums) { implicit request =>
      Project.getPending(author, slug) match {
        case None => Redirect(self.showCreator())
        case Some(pendingProject) =>
          Forms.ProjectSave.bindFromRequest.get.saveTo(pendingProject.project)
          Ok(views.members.config(pendingProject))
      }
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
  def showFirstVersionCreator(author: String, slug: String) = {
    Authenticated(service, forums) { implicit request =>
      Project.getPending(author, slug) match {
        case None => Redirect(self.showCreator())
        case Some(pendingProject) =>
          pendingProject.roles = Forms.MemberRoles.bindFromRequest.get.build()
          val pendingVersion = pendingProject.pendingVersion
          Redirect(routes.Versions.showCreatorWithMeta(
            author, slug, pendingVersion.version.versionString))
      }
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
    ProjectAction(author, slug)(service, forums) { implicit request =>
      val project = request.project
      Conf.debug("isProcessed: " + project.name + " = " + project.isProcessed + " " + project.hashCode())
      stats.projectViewed(implicit request => Ok(views.pages.view(project, project.homePage)))
    }
  }

  /**
    * Submits a flag on the specified project for further review.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       View of project
    */
  def flag(author: String, slug: String) = {
    AuthedProjectAction(author, slug)(service, forums){ implicit request =>
      val user = request.user
      val project = request.project
      if (user.hasUnresolvedFlagFor(project)) {
        // One flag per project, per user at a time
        BadRequest
      } else {
        val reason = FlagReasons(Forms.ProjectFlag.bindFromRequest.get)
        project.flagFor(user, reason)
        Redirect(self.show(author, slug)).flashing("reported" -> "true")
      }
    }
  }

  /**
    * Sets the visible state of the specified Project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param visible  Project visibility
    * @return         Ok
    */
  def setVisible(author: String, slug: String, visible: Boolean) = {
    (AuthedProjectAction(author, slug) andThen ProjectPermissionAction(HideProjects)) { implicit request =>
      request.project.setVisible(visible)
      Ok
    }
  }

  /**
    * Shortcut for navigating to a project.
    *
    * @param pluginId Project pluginId
    * @return Redirect to project page.
    */
  def showProjectWithId(pluginId: String) = Action { implicit request =>
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
      Forms.ProjectSave.bindFromRequest.get.saveTo(request.project)
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
    AuthedProjectAction(author, slug)(service, forums) { implicit request =>
      request.project.setStarredBy(request.user, starred)
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
    ProjectAction(author, slug)(service, forums) { implicit request =>
      stats.projectViewed(implicit request => Ok(views.discuss(request.project)))
    }
  }

  /**
    * Posts a new discussion reply to the forums.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       View of discussion with new post
    */
  def postDiscussionReply(author: String, slug: String) = {
    AuthedProjectAction(author, slug)(service, forums) { implicit request =>
      Forms.ProjectReply.bindFromRequest.fold(
        hasErrors => Redirect(self.showDiscussion(author, slug)).flashing("error" -> hasErrors.errors.head.message),
        content => {
          val error = service.await(forums.Embed.postReply(request.project, request.user, content)).get
          var result = Redirect(self.showDiscussion(author, slug))
          if (error.isDefined) result = result.flashing("error" -> error.get)
          result
        }
      )
    }
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Project manager
    */
  def showSettings(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      Ok(views.settings(request.project))
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
    ProjectAction(author, slug)(service, forums) { implicit request =>
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
    ProjectAction(author, slug)(service, forums) { implicit request =>
      request.project.source match {
        case None => NotFound
        case Some(link) => Redirect(link)
      }
    }
  }

  /**
    * Removes a [[ore.project.ProjectMember]] from the specified project.
    *
    * @param author Project owner
    * @param slug   Project slug
    */
  def removeMember(author: String, slug: String) = {
    SettingsEditAction(author, slug) { implicit request =>
      request.project.removeMember(User.withName(Forms.MemberRemove.bindFromRequest.get.trim).get)
      Redirect(self.showSettings(author, slug))
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
        Redirect(self.showSettings(author, slug)).flashing("error" -> "That name is not available.")
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
      project.delete()
      Redirect(app.showHome(None, None, None))
        .flashing("success" -> ("Project \"" + project.name + "\" deleted."))
    }
  }

}
