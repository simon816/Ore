package controllers

import java.io.File
import javax.inject.Inject

import models.Project
import models.author.{Author, Dev, Team}
import play.Play
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

class Projects @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def create = Action {
    // TODO: Check auth here
    Ok(views.html.project.create(None))
  }

  def postUpload(author: String, name: String) = Action {
    val project = Project.getCached(author, name)
    if (project.isDefined) {
      Ok(views.html.project.create(project))
    } else {
      Redirect(routes.Projects.create())
    }
  }

  /**
    * Attempts to upload and create a new project.
    *
    * @return Result
    */
  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("pluginFile").map { pluginFile =>
      // TODO: Check auth here
      val plugin = PluginFile(new File(Play.application().path() + "/tmp/" + pluginFile.filename))
      pluginFile.ref.moveTo(plugin.getFile)
      val project = plugin.parse
      val result = plugin.getResult
      if (result.isDefined) {
        result.get
      } else if (project.isEmpty) {
        BadRequest("An unknown error occurred.")
      } else {
        val model = project.get
        model.cache()
        Redirect(routes.Projects.postUpload(model.owner.name, model.name))
      }
    }.getOrElse {
      Redirect(routes.Projects.create()).flashing(
        "error" -> "Missing file"
      )
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
      NotFound
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
      NotFound
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
      NotFound
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
      NotFound
    }
  }

}
