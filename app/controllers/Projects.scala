package controllers

import java.nio.file.Files
import javax.inject.Inject

import models.author.{Author, Dev, Team}
import models.{PluginFile, Project}
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
    Ok(views.html.project.create(None))
  }

  /**
    * Uploads a new project for review.
    *
    * @return Result
    */
  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("pluginFile").map { pluginFile =>
      val owner = Dev.get("Spongie").get // TODO: Get auth'd user here
      val plugin = new PluginFile(owner)
      val tmpDir = plugin.getPath.getParent
      if (!Files.exists(tmpDir)) {
        Files.createDirectories(tmpDir)
      }
      pluginFile.ref.moveTo(plugin.getPath.toFile, replace = true)

      var project: Project = null
      try {
        project = plugin.parse
      } catch {
        case e: Exception =>
          BadRequest(e.getMessage)
      }

      project.cache() // Cache for use in postUpload
      Redirect(routes.Projects.postUpload(project.owner.name, project.name))

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
      Ok(views.html.project.create(project))
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
        try {
          file.upload
        } catch {
          case e: Exception =>
            BadRequest(e.getMessage)
        }

        // TODO: Add to DB here
        // Note: Until DB integration the below statement will generate a 404,
        // as desired.
        Redirect(routes.Projects.show(model.owner.name, model.name))
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
      Ok(views.html.project.docs(project.get))
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
      Ok(views.html.project.versions(project.get))
    } else {
      NotFound("No project found.")
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
      Ok(views.html.project.discussion(project.get))
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
    if (author.isDefined) {
      val model = author.get
      model match {
        case dev: Dev =>
          Ok(views.html.project.dev(dev))
        case team: Team =>
          Ok(views.html.project.team(team))
      }
    } else {
      NotFound("No project found.")
    }
  }

}
