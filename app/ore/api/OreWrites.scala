package ore.api

import models.project.{Channel, Project, Version}
import models.user.User
import play.api.libs.json._

/**
  * Contains implicit JSON [[Writes]] for the Ore API.
  */
object OreWrites {

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

}
