package controllers

import db.query.Queries
import db.query.Queries.now
import models.project.Project
import models.user.User
import ore.project.Categories
import ore.project.Categories.Category
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
class Api extends Controller {

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      val channelInfo: Seq[Map[String, String]] = for (channel <- project.channels.seq) yield {
        Map("name" -> channel.name, "color" -> channel.color.hex)
      }

      val members: Seq[Map[String, JsValue]] = for (member <- project.members) yield {
        Map("name" -> JsString(member.name), "roles" -> JsArray(member.roles.map(r => JsString(r.roleType.title)).toSeq))
      }

      val category = project.category
      val rv = project.recommendedVersion

      Json.obj(
        "pluginId" -> project.pluginId,
        "createdAt" -> project.prettyDate,
        "name" -> project.name,
        "owner" -> project.ownerName,
        "description" -> project.description.getOrElse("").toString,
        "href" -> ('/' + project.ownerName + '/' + project.slug),
        "members" -> members,
        "channels" -> channelInfo,
        "recommended" -> Map("channel" -> rv.channel.name, "version" -> rv.versionString),
        "category" -> Map("title" -> category.title, "icon" -> category.icon),
        "views" -> project.views,
        "downloads" -> project.downloads,
        "stars" -> project.stars
      )
    }
  }

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = {
      Json.obj(
        "id" -> user.id,
        "createdAt" -> user.prettyDate,
        "username" -> user.username,
        "roles" -> user.globalRoleTypes.map(_.title),
        "starred" -> user.starred().map(p => p.pluginId)
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
        var categoryArray: Array[Category] = null
        if (categories.isDefined) {
          categoryArray = Categories.fromString(categories.get)
        }
        val projects = now(Queries.Projects.collect(categoryArray, limit.getOrElse(-1), offset.getOrElse(-1))).get
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

  /**
    * Returns a JSON view of Ore Users.
    *
    * @param version  API version
    * @param limit    Amount of users to get
    * @param offset   Offset to drop
    * @return         List of users
    */
  def users(version: String, limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => Ok(Json.toJson(now(Queries.Users.collect(limit.getOrElse(-1), offset.getOrElse(-1))).get))
      case oops => BadRequest
    }
  }

  /**
    * Returns a JSON view of the specified User.
    *
    * @param version    API version
    * @param username   Username of user
    * @return           User with username
    */
  def user(version: String, username: String) = Action {
    version match {
      case "v1" => User.withName(username) match {
        case None => NotFound
        case Some(user) => Ok(Json.toJson(user))
      }
      case sad => BadRequest
    }
  }

}
