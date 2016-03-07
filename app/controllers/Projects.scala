package controllers

import javax.inject.Inject

import models.author.{Author, Dev, Team}
import models.project.Project
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}
import util.PluginFile

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  val user: Author = Team.get("SpongePowered").get // TODO: Replace with auth'd user

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreate = Action {
    // TODO: Check auth here
    Ok(views.html.projects.create(None))
  }

  /**
    * Uploads a new project for review.
    *
    * @return Result
    */
  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("pluginFile") match {
      case None => Redirect(routes.Projects.showCreate()).flashing("error" -> "Missing file")
      case Some(pluginFile) =>
        // Initialize PluginFile
        val plugin = PluginFile.init(pluginFile.ref, this.user)
        // Load plugin metadata
        plugin.loadMeta match {
          case Failure(thrown) => BadRequest(thrown.getMessage)
          case Success(meta) =>
            // TODO: Check against Plugin annotation
            // TODO: Allow ZIPs with Plugin JAR in top level
            val project = Project.fromMeta(this.user, meta)
            if (project.exists) {
              BadRequest("A project of that name already exists.")
            } else {
              project.setPendingUpload(plugin)
              project.cache() // Cache for use in postUpload
              Redirect(routes.Projects.showCreateWithMeta(project.owner.name, project.name))
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
      case None => Redirect(routes.Projects.showCreate())
      case Some(project) => Ok(views.html.projects.create(Some(project)))
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
                  case Failure(thrown) => BadRequest(thrown.getMessage)
                  case Success(void) =>
                    project.create()
                    val channel = project.newChannel("Alpha")
                    channel.newVersion(meta.getVersion)
                    Redirect(routes.Projects.show(author, name))
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
    Author.get(author).getProject(name) match {
      case None => NotFound("No project found.")
      case Some(project) => Ok(views.html.projects.docs(project))
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
    Author.get(author).getProject(name) match {
      case None => NotFound("No project found.")
      case Some(project) => Ok(views.html.projects.versions(project))
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
    Author.get(author).getProject(name) match {
      case None => NotFound("No project found.")
      case Some(project) => Ok(views.html.projects.versionCreate(project, None))
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
    Author.get(author).getProject(name) match {
      case None => NotFound("Project not found.")
      case Some(project) =>
        project.getChannel(channelName) match {
          case None => NotFound("Channel not found.")
          case Some(channel) =>
            channel.getVersion(versionString) match {
              case None => NotFound("Version not found.")
              case Some(version) => Ok.sendFile(PluginFile.getUploadPath(author, name, versionString, channelName).toFile)
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
    Author.get(author).getProject(name) match {
      case None => NotFound("No project found.")
      case Some(project) => Ok(views.html.projects.discussion(project))
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
    val author = Author.get(name)
    if (author.isRegistered) {
      author match {
        case dev: Dev =>
          Ok(views.html.projects.dev(dev))
        case team: Team =>
          Ok(views.html.projects.team(team))
      }
    } else {
      NotFound("No user found.")
    }
  }

}
