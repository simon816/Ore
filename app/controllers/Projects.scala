package controllers

import javax.inject.Inject

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
    val project = models.Project.get(author, id)
    if (project.isDefined) {
      Ok(views.html.project.detail(project.get))
    } else {
      NotFound
    }
  }

}
