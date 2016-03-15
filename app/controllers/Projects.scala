package controllers

import javax.inject.Inject

import controllers.routes.{Projects => self}
import db.Storage
import models.project.Project.PendingProject
import models.project.{Channel, Project, Version}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import plugin.ProjectManager
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport with Secured {

  private def withProject(author: String, name: String, f: Project => Result): Result = {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) => f(project)
    }
  }

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreate = withAuth { context => implicit request =>
    Ok(views.projects.create(None))
  }

  /**
    * Uploads a Project's first plugin file.
    *
    * @return Result
    */
  def upload = { withUser(None, user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreate()).flashing("error" -> "Missing file")
      case Some(tmpFile) =>
        // Initialize plugin file
        ProjectManager.initUpload(tmpFile.ref, user) match {
          case Failure(thrown) => throw thrown
          case Success(plugin) =>
            // Cache pending project for later use
            val meta = plugin.getMeta.get
            println("meta = " + meta)
            val project = Project.fromMeta(user.username, meta)
            if (project.exists) {
              BadRequest("You already have a project named " + meta.getName + "!")
            } else {
              Project.setPending(project, plugin)
              Redirect(self.showCreateWithMeta(project.owner, project.name))
            }
        }
    })
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author Author of plugin
    * @param name Name of plugin
    * @return Create project view
    */
  def showCreateWithMeta(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => Redirect(self.showCreate())
      case Some(pending) => Ok(views.projects.create(Some(pending)))
    })
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project
    *
    * @param author Author of project
    * @param name Name of project
    * @return Redirection to project page if successful
    */
  def showFirstVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val pendingVersion = pendingProject.initFirstVersion
        Redirect(self.showVersionCreateWithMeta(author, name, pendingVersion.channelName, pendingVersion.version.versionString))
    })
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def show(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => Ok(views.projects.docs(project)))
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def showVersions(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) =>
          Storage.now(project.getVersions) match {
            case Failure(thrown) => throw thrown
            case Success(versions) => Ok(views.projects.versions(project, channels, versions));
          }
      }
    })
  }

  /**
    * Shows the creation form for new versions on existing projects.
    *
    * @param author Owner of project
    * @param name Name of project
    * @return Version creation view
    */
  def showVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Ok(views.projects.versionCreate(project, None, showFileControls = true))))
  }

  def uploadVersion(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showVersionCreate(author, name)).flashing("error" -> "Missing file")
      case Some(tmpFile) =>
        // Get project
        withProject(author, name, project => {
          // Initialize plugin file
          ProjectManager.initUpload(tmpFile.ref, user) match {
            case Failure(thrown) => throw thrown
            case Success(plugin) =>
              // Cache version for later use
              val meta = plugin.getMeta.get
              val version = Version.fromMeta(project, meta)
              val channelName = Channel.getSuggestedNameForVersion(version.versionString)
              Version.setPending(author, name, channelName, version, plugin)
              Redirect(self.showVersionCreateWithMeta(author, name, channelName, version.versionString))
          }
        })
    })
  }

  private def pendingOrReal(author: String, name: String): Option[Any] = {
    Storage.now(Storage.optProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(projectOpt) => projectOpt match {
        case None => Project.getPending(author, name)
        case Some(project) => Some(project)
      }
    }
  }

  def showVersionCreateWithMeta(author: String, name: String,
                                channelName: String, versionString: String) = {
                                withUser(Some(author), user => implicit request =>
    // Get pending version
    Version.getPending(author, name, channelName, versionString) match {
      case None => Redirect(routes.Application.index())
      case Some(pendingVersion) =>
        // Get project
        pendingOrReal(author, name) match {
          case None => Redirect(routes.Application.index())
          case Some(p) => p match {
            case pending: PendingProject =>
              Ok(views.projects.versionCreate(pending.project, Some(pendingVersion), showFileControls = false))
            case real: Project =>
              Ok(views.projects.versionCreate(real, Some(pendingVersion), showFileControls = true))
          }
        }
    })
  }

  def createVersion(author: String, name: String, channelName: String, versionString: String) = {
                    withUser(Some(author), user => implicit request =>
    Version.getPending(author, name, channelName, versionString) match {
      case None => BadRequest("No version to create.")
      case Some(pendingVersion) =>
        // Check for pending project
        Project.getPending(author, name) match {
          case None => pendingVersion.complete match {
            case Failure(thrown) => throw thrown
            case Success(void) => Redirect(self.show(author, name))
          }
          case Some(pendingProject) => pendingProject.complete match {
            case Failure(thrown) => throw thrown
            case Success(void) => Redirect(self.show(author, name))
          }
      }
    })
  }

  /**
    * Sends the specified Project Version.
    *
    * @param author Project owner
    * @param name Project name
    * @param channelName Version channel
    * @param versionString Version string
    * @return Sent file
    */
  def downloadVersion(author: String, name: String, channelName: String, versionString: String) = Action {
    withProject(author, name, project => {
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound("Channel not found.")
          case Some(channel) =>
            Storage.now(channel.getVersion(versionString)) match {
              case Failure(thrown) => throw thrown
              case Success(versionOpt) => versionOpt match {
                case None => NotFound("Version not found.")
                case Some(version) => Ok.sendFile(ProjectManager.getUploadPath(author, name, versionString, channelName).toFile)
              }
            }
        }
      }
    })
  }

  def downloadRecommendedVersion(author: String, name: String) = Action {
    withProject(author, name, project => {
      Storage.now(project.getRecommendedVersion) match {
        case Failure(thrown) => throw thrown
        case Success(version) =>
          Storage.now(version.getChannel) match {
            case Failure(thrown) => throw thrown
            case Success(channel) =>
              Ok.sendFile(ProjectManager.getUploadPath(author, name, version.versionString, channel.name).toFile)
          }
      }
    })
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def showDiscussion(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => Ok(views.projects.discussion(project)))
  }

  val renameForm = Form(single("name" -> text))

  def showManager(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Ok(views.projects.manage(project))))
  }

  def rename(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      val newName = this.renameForm.bindFromRequest.get
      Storage.now(project.setName(newName)) match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(self.show(author, newName))
      }
    }))
  }

  def delete(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      project.delete match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(routes.Application.index())
      }
    }))
  }

  /**
    * Displays an author page for the specified name. This can be either a Team
    * or a Dev.
    *
    * @param name Name of author
    * @return View of author
    */
  def showAuthor(name: String) = Action { implicit request =>
    NotFound("TODO")
  }

}
