package controllers

import javax.inject.Inject

import controllers.routes.{Projects => self}
import models.author.Author
import models.author.Author.Unknown
import models.project.Project
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import sql.Storage
import util.PluginFile
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport {

  // TODO: Replace with auth'd user
  val user: Author = Unknown(name="SpongePowered")

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreate = Action {
    // TODO: Check auth here
    Ok(views.projects.create(None))
  }

  /**
    * Uploads a new project for review.
    *
    * @return Result
    */
  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("pluginFile") match {
      case None => Redirect(self.showCreate()).flashing("error" -> "Missing file")
      case Some(pluginFile) =>
        // Initialize PluginFile
        val plugin = PluginFile.init(pluginFile.ref, this.user)
        // Load plugin metadata
        plugin.loadMeta match {
          case Failure(thrown) => throw thrown
          case Success(meta) =>
            // TODO: Allow ZIPs with Plugin JAR in top level
            val project = Project.fromMeta(this.user.name, meta)
            if (project.exists) {
              BadRequest("A project of that name already exists.")
            } else {
              project.setPendingUpload(plugin)
              project.cache() // Cache for use in postUpload
              Redirect(self.showCreateWithMeta(project.owner, project.name))
            }
        }
    }
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author Author of plugin
    * @param name Name of plugin
    * @return Create project view
    */
  def showCreateWithMeta(author: String, name: String) = Action {
    // TODO: Check auth here
    Project.getCached(author, name) match {
      case None => Redirect(self.showCreate())
      case Some(project) => Ok(views.projects.create(Some(project)))
    }
  }

  /**
    * Creates the cached project with the specified author and name.
    *
    * @param author Author of project
    * @param name Name of project
    * @return Redirection to project page if successful
    */
  def create(author: String, name: String) = Action {
    // TODO: Check auth here
    // TODO: Encapsulate this mess
    Project.getCached(author, name) match {
      case None => BadRequest("No project found.")
      case Some(project) =>
        project.free() // Release from cache
        project.getPendingUpload match {
          case None => BadRequest("No file pending.")
          case Some(pluginFile) =>
            pluginFile.getMeta match {
              case None => BadRequest("No meta info found for plugin.")
              case Some(meta) =>
                pluginFile.upload match {
                  case Failure(thrown) => throw thrown
                  case Success(void) =>
                    Storage.now(Storage.createProject(project)) match {
                      case Failure(thrown) => throw thrown
                      case Success(newProject) =>
                        Storage.now(newProject.newChannel("Alpha")) match {
                          case Failure(thrown) => throw thrown
                          case Success(channel) =>
                            Storage.now(channel.newVersion(meta.getVersion)) match {
                              case Failure(thrown) => throw thrown
                              case Success(version) => Redirect(self.show(author, name))
                            }
                        }
                    }
                }
            }
        }
    }
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def show(author: String, name: String) = Action {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) => Ok(views.projects.docs(project))
    }
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def showVersions(author: String, name: String) = Action {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) => Ok(views.projects.versions(project))
    }
  }

  /**
    * Shows the creation form for new versions on existing projects.
    *
    * @param author Owner of project
    * @param name Name of project
    * @return Version creation view
    */
  def showVersionCreate(author: String, name: String) = Action {
    // TODO: Check auth here
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) => Ok(views.projects.versionCreate(project, None))
    }
  }

  def uploadVersion(author: String, name: String) = Action(parse.multipartFormData) { request =>
    NotFound("TODO")
  }

  def showVersionCreateWithMeta(author: String, name: String, channel: String, versionString: String) = Action {
    NotFound("TODO")
  }

  def createVersion(author: String, name: String, channel: String, versionString: String) = Action {
    NotFound("TODO")
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
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) =>
        Storage.now(project.getChannel(channelName)) match {
          case Failure(thrown) => throw thrown
          case Success(channelOpt) => channelOpt match {
            case None => NotFound("Channel not found.")
            case Some(channel) =>
              Storage.now(channel.getVersion(versionString)) match {
                case Failure(thrown) => throw thrown
                case Success(versionOpt) => versionOpt match {
                  case None => NotFound("Version not found.")
                  case Some(version) => Ok.sendFile(PluginFile.getUploadPath(author, name, versionString, channelName).toFile)
                }
              }
          }
        }
    }
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author Owner of project
    * @param name   Name of project
    * @return View of project
    */
  def showDiscussion(author: String, name: String) = Action {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(project) => Ok(views.projects.discussion(project))
    }
  }

  /**
    * Displays an author page for the specified name. This can be either a Team
    * or a Dev.
    *
    * @param name Name of author
    * @return View of author
    */
  def showAuthor(name: String) = Action {
    NotFound("TODO")
  }

}
