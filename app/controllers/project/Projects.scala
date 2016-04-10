package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Projects => self}
import controllers.routes.{Application => app}
import models.project._
import pkg.{Categories, InvalidPluginFileException, ProjectManager}
import play.api.i18n.MessagesApi
import play.api.mvc._
import util.Forms
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * Controller for handling Project related actions.
  *
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends BaseController {

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreator = withAuth { context => implicit request =>
    Ok(views.projects.create(None))
  }

  /**
    * Uploads a Project's first plugin file for further processing.
    *
    * @return Result
    */
  def upload = { withUser(None, user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreator()).flashing("error" -> "No file submitted.")
      case Some(tmpFile) =>
        // Initialize plugin file
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
            val project = Project.fromMeta(user.username, meta)
            Project.setPending(project, plugin)
            Redirect(self.showCreatorWithMeta(project.ownerName, project.slug))
        }
    })
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author   Author of plugin
    * @param slug     Project slug
    * @return         Create project view
    */
  def showCreatorWithMeta(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, slug) match {
      case None => Redirect(self.showCreator())
      case Some(pending) => Ok(views.projects.create(Some(pending)))
    })
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author   Author of project
    * @param slug     Project slug
    * @return         Redirection to project page if successful
    */
  def showFirstVersionCreator(author: String, slug: String) = { withUser(Some(author), user => implicit request => {
    Project.getPending(author, slug) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get.trim)
        pendingProject.project.category = category
        val pendingVersion = pendingProject.initFirstVersion
        Redirect(routes.Versions.showCreatorWithMeta(
          author, slug, pendingVersion.channelName, pendingVersion.version.versionString
        ))
    }})
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @return         View of project
    */
  def show(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => {
      Ok(views.projects.pages.view(project, project.homePage))
    }, countView = true)
  }

  /**
    * Saves the specified Project from the settings manager.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of project
    */
  def save(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get)
      if (!category.equals(project.category)) {
        project.category = category
      }
      Redirect(self.show(author, slug))
    }))
  }

  /**
    * Sets the "starred" status of a Project for the current user.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param starred  True if should set to starred
    * @return         Result code
    */
  def setStarred(author: String, slug: String, starred: Boolean) = { withUser(None, user => implicit request =>
    withProject(author, slug, project => {
      val alreadyStarred = project.isStarredBy(user)
      if (starred) {
        if (!alreadyStarred) {
          project.starFor(user)
        }
      } else if (alreadyStarred) {
        project.unstarFor(user)
      }
      Ok
    }))
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @return         View of project
    */
  def showDiscussion(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => Ok(views.projects.discuss(project)), countView = true)
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Project manager
    */
  def showManager(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => Ok(views.projects.manage(project)), countView = true))
  }

  /**
    * Redirect's to the project's issue tracker if any.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Issue tracker
    */
  def showIssues(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => {
      project.issues match {
        case None => NotFound
        case Some(link) => Redirect(link)
      }
    })
  }

  /**
    * Redirect's to the project's source code if any.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Source code
    */
  def showSource(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => {
      project.source match {
        case None => NotFound
        case Some(link) => Redirect(link)
      }
    })
  }

  /**
    * Renames the specified project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Project homepage
    */
  def rename(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val newName = Project.sanitizeName(Forms.ProjectRename.bindFromRequest.get)
      if (!Project.isNamespaceAvailable(author, Project.slugify(newName))) {
        Redirect(self.showManager(author, slug)).flashing("error" -> "That name is not available.")
      } else {
        project.name = newName
        Redirect(self.show(author, project.slug))
      }
    }))
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Home page
    */
  def delete(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      project.delete.get
      Redirect(app.showHome(None))
        .flashing("success" -> ("Project \"" + project.name + "\" deleted."))
    }))
  }

}
