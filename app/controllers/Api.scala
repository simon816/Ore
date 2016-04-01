package controllers

import db.Storage
import models.project.{Categories, Project}
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Success}

class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      var channelNames: Seq[String] = null
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) => channelNames = for (channel <- channels) yield channel.getName
      }

      Json.obj(
        "pluginId" -> project.pluginId,
        "createdAt" -> project.prettyDate,
        "name" -> project.getName,
        "owner" -> project.owner,
        "authors" -> project.authors,
        "channels" -> channelNames,
        "category" -> Categories(project.categoryId).title,
        "views" -> project.views,
        "downloads" -> project.downloads,
        "starred" -> project.starred
      )
    }
  }

  /**
    * Returns a JSON view of all projects.
    *
    * @param version  API version
    * @return         JSON view of projects
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

  /**
    * Returns a JSON view of a Project meeting the specified criteria.
    *
    * @param version    API version
    * @param pluginId   Plugin ID of Project to find
    * @return           JSON view of project if found, 404 otherwise
    */
  def search(version: String, pluginId: Option[String]) = Action {
    pluginId match {
      case None => NotFound
      case Some(id) => Storage.now(Storage.optProject(id)) match {
        case Failure(thrown) => throw thrown
        case Success(optProject) => optProject match {
          case None => NotFound
          case Some(project) => version match {
            case "v1" => Ok(Json.toJson(project))
            case yikes => NotFound
          }
        }
      }
    }
  }

}
