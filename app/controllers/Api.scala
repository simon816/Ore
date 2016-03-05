package controllers

import models.Project
import play.api.libs.json._
import play.api.mvc._

class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = Json.obj(
      "id" -> project.id,
      "name" -> project.name,
      "description" -> project.description,
      "owner" -> project.owner.name,
      "views" -> project.views,
      "downloads" -> project.downloads,
      "starred" -> project.starred
    )
  }

  def listProjects(version: String) = Action {
    version match {
      case "v1" => Ok(Json.toJson(Project.projects))
      case zoinks => NotFound
    }
  }

  def listProjects() = listProjects("v1")

}
