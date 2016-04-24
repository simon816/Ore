package controllers

import db.OrePostgresDriver.api._
import db.query.Queries
import db.query.Queries.now
import models.project.{Channel, Project, Version}
import models.user.User
import ore.project.Categories.Category
import ore.project.{Categories, ProjectSortingStrategies}
import play.api.libs.json._
import play.api.mvc._

/**
  * Ore API (v1)
  */
class Api extends Controller {

  implicit val channelWrites = new Writes[Channel] {
    def writes(channel: Channel) = Json.obj("name" -> channel.name, "color" -> channel.color.hex)
  }

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      val members: List[JsObject] = for (member <- project.members) yield {
        Json.obj(
          "name" -> JsString(member.name),
          "roles" -> JsArray(member.roles.map(r => JsString(r.roleType.title)).toSeq)
        )
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
        "channels" -> Json.toJson(project.channels.seq),
        "recommended" -> Json.obj("channel" -> rv.channel.name, "version" -> rv.versionString),
        "category" -> Json.obj("title" -> category.title, "icon" -> category.icon),
        "views" -> project.views,
        "downloads" -> project.downloads,
        "stars" -> project.stars
      )
    }
  }

  implicit val versionWrites = new Writes[Version] {
    def writes(version: Version) = {
      val project = version.project
      val dependencies: List[JsObject] = version.dependencies.map { dependency =>
        Json.obj("pluginId" -> dependency.pluginId, "version" -> dependency.version)
      }

      Json.obj(
        "id" -> version.id.get,
        "createdAt" -> version.prettyDate,
        "name" -> version.versionString,
        "dependencies" -> dependencies,
        "pluginId" -> project.pluginId,
        "channel" -> Json.toJson(project.channels.withId(version.channelId).get),
        "fileSize" -> version.fileSize
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
  def listProjects(version: String, categories: Option[String], sort: Option[Int], q: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" =>
        val categoryArray: Array[Category] = categories.map(Categories.fromString).orNull
        val s = sort.map(ProjectSortingStrategies.withId(_).get).getOrElse(ProjectSortingStrategies.Default)
        val filter = q.map(Queries.Projects.searchFilter).orNull
        val lim = Math.max(limit.getOrElse(Project.InitialLoad), Project.InitialLoad)
        val f = Queries.Projects.collect(filter, categoryArray, lim, offset.getOrElse(-1), s)
        val projects = now(f).get
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
  def showProject(version: String, pluginId: String) = Action {
    version match {
      case "v1" => Project.withPluginId(pluginId) match {
        case None => NotFound
        case Some(project) => Ok(Json.toJson(project))
      }
      case yikes => BadRequest
    }
  }

  def listVersions(version: String, pluginId: String, channels: Option[String],
                   limit: Option[Int], offset: Option[Int]) = Action {
    version match {
      case "v1" => Project.withPluginId(pluginId) match {
        case None => NotFound
        case Some(project) =>
          val channelIds: Option[Seq[Int]] = channels.map(_.toLowerCase.split(',').map { name =>
            project.channels.find(_.name.toLowerCase === name).get.id.get
          })
          val filter = channelIds.map(Queries.Versions.channelFilter).orNull
          val lim = Math.max(limit.getOrElse(Version.InitialLoad), Version.InitialLoad)
          val versions = project.versions.sorted(filter, _.createdAt.desc, lim, offset.getOrElse(-1))
          Ok(Json.toJson(versions))
      }
      case gorp => BadRequest
    }
  }

  def showVersion(version: String, pluginId: String, channel: String, name: String) = Action {
    version match {
      case "v1" => Project.withPluginId(pluginId) match {
        case None => NotFound
        case Some(project) => project.channels.find(_.name.toLowerCase === channel.toLowerCase) match {
          case None => NotFound
          case Some(chan) => chan.versions.find(_.versionString === name) match {
            case None => NotFound
            case Some(v) => Ok(Json.toJson(v))
          }
        }
        case fffffs => BadRequest
      }
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
  def listUsers(version: String, limit: Option[Int], offset: Option[Int]) = Action {
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
  def showUser(version: String, username: String) = Action {
    version match {
      case "v1" => User.withName(username) match {
        case None => NotFound
        case Some(user) => Ok(Json.toJson(user))
      }
      case sad => BadRequest
    }
  }

}
