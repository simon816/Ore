package controllers

import _root_.pkg.Categories
import db.query.Queries
import db.query.Queries.now
import models.project.Project
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      val channelInfo: Seq[Map[String, String]] = for (channel <- project.channels) yield {
        Map("name" -> channel.name, "color" -> channel.color.hex)
      }
      val category = project.category
      Json.obj(
        "pluginId" -> project.pluginId,
        "createdAt" -> project.prettyDate,
        "name" -> project.name,
        "owner" -> project.ownerName,
        "authors" -> project.authorNames,
        "channels" -> channelInfo,
        "category" -> Map("title" -> category.title, "icon" -> category.icon),
        "views" -> project.views,
        "downloads" -> project.downloads,
        "stars" -> project.stars
      )
    }
  }

  /**
    * Returns a JSON view of all projects.
    *
    * @param version    API version
    * @return           JSON view of projects
    */
  def projects(version: String, categories: Option[String], limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" =>
        var categoryIds: Array[Int] = null
        if (categories.isDefined) {
          categoryIds = Categories.fromString(categories.get).map(_.id)
        }
        val projects = now(Queries.Projects.collect(categoryIds, limit.getOrElse(-1), offset.getOrElse(-1))).get
        Ok(Json.toJson(projects))
      case zoinks => BadRequest
    }
  }

  /**
    * Returns a JSON view of a Project.
    *
    * @param version    API version
    * @param pluginId   Plugin ID of project
    * @return           Project with Plugin ID
    */
  def project(version: String, pluginId: Option[String]) = Action {
    version match {
      case "v1" => pluginId match {
        case None => NotFound
        case Some(id) => Project.withPluginId(id) match {
          case None => NotFound
          case Some(project) => Ok(Json.toJson(project))
        }
      }
      case yikes => BadRequest
    }
  }

}
