package controllers

import javax.inject.Inject

import models.Project
import models.author.{Author, Dev, Team}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller}

class Projects @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def create = Action {
    Ok(views.html.project.create())
  }

  /**
    * Attempts to upload and create a new project.
    *
    * @return Result
    */
  def upload = Action(parse.multipartFormData) { request =>
    request.body.file("pluginFile").map { pluginFile =>
      // TODO: Check plugin meta file here for plugin details
      Ok("File uploaded")
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
