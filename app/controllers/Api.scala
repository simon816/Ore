package controllers

import db.Storage
import models.project.Project
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Success}

class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = Json.obj(
      "id" -> project.pluginId,
      "name" -> project.name,
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
    Storage.now(Storage.getProjects) match {
      case Failure(thrown) => throw thrown
      case Success(projects) => version match {
        case "v1" => Ok(Json.toJson(projects))
        case zoinks => NotFound
      }
    }
  }

}
