package controllers

import db.Storage
import models.project.{Categories, Project}
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Success}

class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      var channelInfo: Seq[Map[String, String]] = null
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) => channelInfo = for (channel <- channels) yield {
          Map("name" -> channel.getName, "color" -> channel.getColor.hex)
        }
      }

      Json.obj(
        "pluginId" -> project.pluginId,
        "createdAt" -> project.prettyDate,
        "name" -> project.getName,
        "owner" -> project.owner,
        "authors" -> project.authors,
        "channels" -> channelInfo,
        "category" -> Categories(project.getCategory.id).title,
        "views" -> project.getViews,
        "downloads" -> project.getDownloads,
        "starred" -> project.getStars
      )
    }
  }

  /**
    * Returns a JSON view of all projects.
    *
    * @param version    API version
    * @return           JSON view of projects
    */
  def listProjects(version: String) = Action {
    version match {
      case "v1" => Storage.now(Storage.getProjects) match {
        case Failure(thrown) => throw thrown
        case Success(projects) => Ok(Json.toJson(projects))
      }
      case zoinks => BadRequest
    }
  }

  def getProject(version: String, pluginId: Option[String]) = Action {
    version match {
      case "v1" => pluginId match {
        case None => NotFound
        case Some(id) => Storage.now(Storage.optProject(id)) match {
          case Failure(thrown) => throw thrown
          case Success(optProject) => optProject match {
            case None => NotFound
            case Some(project) => Ok(Json.toJson(project))
          }
        }
        case yikes => BadRequest
      }
    }
  }

}
