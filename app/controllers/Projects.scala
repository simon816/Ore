package controllers

import javax.inject.Inject

import models.Project
import models.author.{Dev, Team}
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

  def show(author: String, id: String) = Action {
    val project =  Project.get(author, id)
    if (project.isDefined) {
      Ok(views.html.project.docs(project.get))
    } else {
      NotFound
    }
  }

  def showVersions(author: String, id: String) = Action {
    val project = Project.get(author, id)
    if (project.isDefined) {
      Ok(views.html.project.versions(project.get))
    } else {
      NotFound
    }
  }

  def showDiscussion(author: String, id: String) = Action {
    val project = Project.get(author, id)
    if (project.isDefined) {
      Ok(views.html.project.discussion(project.get))
    } else {
      NotFound
    }
  }

  def showDev(name: String) = Action {
    val dev = Dev.get(name)
    if (dev.isDefined) {
      Ok(views.html.project.author.dev(dev.get))
    } else {
      NotFound
    }
  }

  def showTeam(name: String) = Action {
    val team = Team.get(name)
    if (team.isDefined) {
      Ok(views.html.project.author.team(team.get))
    } else {
      NotFound
    }
  }

}
