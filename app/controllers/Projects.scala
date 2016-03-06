package controllers

import javax.inject.Inject

import models.author.{Author, Dev, Team}
import models.project.Project
import models.util.PluginFile
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

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
    request.body.file("pluginFile").map { pluginFile =>
      // Initialize PluginFile
      val owner = Team.get("SpongePowered").get // TODO: Get auth'd user here
      val plugin = PluginFile.init(pluginFile.ref, owner)

      // Load plugin metadata
      var meta: PluginMetadata = null
      var error: String = null
      try {
        meta = plugin.loadMeta
      } catch {
        case e: Exception =>
          error = e.getMessage
      }

      if (error != null) {
        BadRequest(error)
      } else {
        // TODO: More file validation
        // TODO: Zip "bundle" uploads
        // TODO: Make sure ID is unique
        val project = Project.fromMeta(owner, meta)
        if (project.exists) {
          BadRequest("A project of that name already exists.")
        } else {
          project.setPendingUpload(plugin)
          project.cache() // Cache for use in postUpload
          Redirect(routes.Projects.postUpload(project.owner.name, project.name))
        }
      }

    }.getOrElse {
      Redirect(routes.Projects.showCreate()).flashing(
        "error" -> "Missing file"
      )
    }
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author Author of plugin
    * @param name Name of plugin
    * @return Create project view
    */
  def postUpload(author: String, name: String) = Action {
    // TODO: Check auth here
    val project = Project.getCached(author, name)
    if (project.isDefined) {
      Ok(views.html.projects.create(project))
    } else {
      Redirect(routes.Projects.showCreate())
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
    val project = Project.getCached(author, name)
    if (project.isDefined) {
      val model = project.get
      model.free() // Release from cache

      val pending = model.getPendingUpload
      if (pending.isEmpty) {
        BadRequest("No file pending.")
      } else {
        val file = pending.get
        if (file.getMeta.isEmpty) {
          BadRequest("No meta info found for plugin.")
        } else {
          var error: String = null
          try {
            file.upload()
          } catch {
            case e: Exception =>
              error = e.getMessage
          }

          if (error != null) {
            BadRequest(error)
          } else {
            error = null
            try {
              model.create()
            } catch {
              case e: Exception =>
                error = e.getMessage
            }

            if (error != null) {
              BadRequest(error)
            } else {
              // Add version to project
              val meta = file.getMeta.get
              val channel = model.newChannel("Alpha") // TODO: Channel selection (plugin-meta maybe?)
              channel.newVersion(meta.getVersion)
              Redirect(routes.Projects.show(model.owner.name, model.name))
            }
          }
        }
      }
    } else {
      BadRequest("No project to post.")
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
    val project = Project.get(author, name)
    if (project.isDefined) {
      Ok(views.html.projects.docs(project.get))
    } else {
      NotFound("No project found.")
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
    val project = Project.get(author, name)
    if (project.isDefined) {
      Ok(views.html.projects.versions(project.get))
    } else {
      NotFound("No project found.")
    }
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
    val project = Project.get(author, name)
    if (project.isDefined) {
      val model = project.get
      val channel = model.getChannel(channelName)
      if (channel.isDefined) {
        val version = channel.get.getVersion(versionString)
        if (version.isDefined) {
          Ok.sendFile(PluginFile.getUploadPath(author, name, versionString, channelName.toUpperCase).toFile)
        } else {
          NotFound("Version not found.")
        }
      } else {
        NotFound("Channel not found.")
      }
    } else {
      NotFound("Project not found.")
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
    val project = Project.get(author, name)
    if (project.isDefined) {
      Ok(views.html.projects.discussion(project.get))
    } else {
      NotFound("No project found.")
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
      NotFound("No project found.")
    }
  }

}
