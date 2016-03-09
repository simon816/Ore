package controllers

import models.project.Project
import play.api.libs.json._
import play.api.mvc._
import sql.Storage

class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = Json.obj(
      "id" -> project.pluginId,
      "name" -> project.name,
      "description" -> project.description,
      "owner" -> project.owner,
      "views" -> project.views,
      "downloads" -> project.downloads,
      "starred" -> project.starred
    )
  }

  /**
    * Returns a JSON view of all projects.
    *
    * @param version API version
    * @return JSON view of projects
    */
  def listProjects(version: String) = Action {
    version match {
      case "v1" => Ok(Json.toJson(Storage.getProjects))
      case zoinks => NotFound
    }
  }

}
