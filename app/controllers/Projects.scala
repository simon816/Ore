package controllers

import javax.inject.Inject

import controllers.routes.{Projects => self}
import db.Storage
import models.project.Project.PendingProject
import models.project.{Category, Channel, Project, Version}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import plugin.{Pages, ProjectManager}
import util.Forms
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport with Secured {

  private def withProject(author: String, name: String, f: Project => Result): Result = {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => NotFound
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
    * Uploads a Project's first plugin file for further processing.
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
            val project = Project.fromMeta(user.username, meta)
            Project.setPending(project, plugin)
            Redirect(self.showCreateWithMeta(project.owner, project.getName))
        }
    })
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author   Author of plugin
    * @param name     Name of plugin
    * @return         Create project view
    */
  def showCreateWithMeta(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => Redirect(self.showCreate())
      case Some(pending) => Ok(views.projects.create(Some(pending)))
    })
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author   Author of project
    * @param name     Name of project
    * @return         Redirection to project page if successful
    */
  def showFirstVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val categoryId = Category.withName(Forms.ProjectCategory.bindFromRequest.get).id
        pendingProject.project.categoryId = categoryId
        val pendingVersion = pendingProject.initFirstVersion
        Redirect(self.showVersionCreateWithMeta(author, name, pendingVersion.channelName, pendingVersion.version.versionString))
    })
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         View of project
    */
  def show(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => {
      Ok(views.projects.docs(project, Pages.getHome(author, name)))
    })
  }

  /**
    * Displays the documentation page editor for the specified project and page
    * name.
    *
    * @param author   Owner name
    * @param name     Project name
    * @param page     Page name
    * @return         Page editor
    */
  def showPageEdit(author: String, name: String, page: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      Ok(views.projects.pageEdit(project, page, Pages.getOrCreate(author, name, page).getContents))
    }))
  }

  /**
    * Saves changes made on a documentation page.
    *
    * @param author   Owner name
    * @param name     Project name
    * @param page     Page name
    * @return         Project home
    */
  def savePage(author: String, name: String, page: String) = withUser(Some(author), user => implicit request => {
    // TODO: Validate content size and title
    // TODO: Limit number of pages allowed
    val pageForm = Forms.PageEdit.bindFromRequest.get
    Pages.getOrCreate(author, name, page).update(pageForm._1, pageForm._2)
    Redirect(self.showPage(author, name, page))
  })

  def deletePage(author: String, name: String, page: String) = withUser(Some(author), user => implicit request => {
    Pages.delete(author, name, page)
    Redirect(self.show(author, name))
  })

  def showPage(author: String, name: String, page: String) = Action { implicit request =>
    withProject(author, name, project => {
      Pages.get(author, name, page) match {
        case None => NotFound
        case Some(page) => Ok(views.projects.docs(project, page))
      }
    })
  }

  /**
    * Shows the specified version detail page.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version detail view
    */
  def showVersion(author: String, name: String, channelName: String, versionString: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound
          case Some(channel) => Storage.now(channel.getVersion(versionString)) match {
            case Failure(thrown) => throw thrown
            case Success(versionOpt) => versionOpt match {
              case None => NotFound
              case Some(version) => Ok(views.projects.versionDetail(project, channel, version))
            }
          }
        }
      }
    })
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         View of project
    */
  def showVersions(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) =>
          Ok(views.projects.versions(project, channels, project.getVersions));
      }
    })
  }

  /**
    * Shows the creation form for new versions on projects.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         Version creation view
    */
  def showVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Ok(views.projects.versionCreate(project, None, showFileControls = true))))
  }

  /**
    * Uploads a new version for a project for further processing.
    *
    * @param author   Owner name
    * @param name     Project name
    * @return         Version create page (with meta)
    */
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
    // Returns either a PendingProject or existing Project
    Storage.now(Storage.optProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(projectOpt) => projectOpt match {
        case None => Project.getPending(author, name)
        case Some(project) => Some(project)
      }
    }
  }

  /**
    * Displays the "version create" page with the associated plugin meta-data.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version create view
    */
  def showVersionCreateWithMeta(author: String, name: String,
                                channelName: String, versionString: String) = {
                                withUser(Some(author), user => implicit request =>
    // Get pending version
    Version.getPending(author, name, channelName, versionString) match {
      case None => Redirect(routes.Application.index(None))
      case Some(pendingVersion) =>
        // Get project
        pendingOrReal(author, name) match {
          case None => Redirect(routes.Application.index(None))
          case Some(p) => p match {
            case pending: PendingProject =>
              Ok(views.projects.versionCreate(pending.project, Some(pendingVersion), showFileControls = false))
            case real: Project =>
              Ok(views.projects.versionCreate(real, Some(pendingVersion), showFileControls = true))
          }
        }
    })
  }

  /**
    * Completes the creation of the specified pending version or project if
    * first version.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               New version view
    */
  def createVersion(author: String, name: String, channelName: String, versionString: String) = {
                    withUser(Some(author), user => implicit request =>
    Version.getPending(author, name, channelName, versionString) match {
      case None => BadRequest("No version to create.")
      case Some(pendingVersion) =>
        // Check for pending project
        Project.getPending(author, name) match {
          case None => pendingVersion.complete match {
            case Failure(thrown) => throw thrown
            case Success(void) => Redirect(self.showVersion(author, name, channelName, versionString))
          }
          case Some(pendingProject) => pendingProject.complete match {
            case Failure(thrown) => throw thrown
            case Success(void) => Redirect(self.show(author, name))
          }
      }
    })
  }

  /**
    * Deletes the specified version and returns to the version page.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Versions page
    */
  def deleteVersion(author: String, name: String, channelName: String, versionString: String) = {
                    withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      println("project = " + project.getName)
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound("Channel not found.")
          case Some(channel) => Storage.now(channel.getVersion(versionString)) match {
            case Failure(thrown) => throw thrown
            case Success(versionOpt) => versionOpt match {
              case None => NotFound("Version not found.")
              case Some(version) => channel.deleteVersion(version, project) match {
                case Failure(thrown) => throw thrown
                case Success(void) => Redirect(self.showVersions(author, name))
              }
            }
          }
        }
      }
    }))
  }

  /**
    * Sends the specified Project Version to the client.
    *
    * @param author         Project owner
    * @param name           Project name
    * @param channelName    Version channel
    * @param versionString  Version string
    * @return               Sent file
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

  /**
    * Sends the specified project's current recommended version to the client.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Sent file
    */
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
    * @param author   Owner of project
    * @param name     Name of project
    * @return         View of project
    */
  def showDiscussion(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => Ok(views.projects.discussion(project)))
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Project manager
    */
  def showManager(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Ok(views.projects.manage(project))))
  }

  /**
    * Renames the specified project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Project homepage
    */
  def rename(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      val newName = Forms.ProjectRename.bindFromRequest.get
      Storage.now(project.setName(newName)) match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(self.show(author, newName))
      }
    }))
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Home page
    */
  def delete(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      project.delete match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(routes.Application.index(None))
      }
    }))
  }

}
