package ore.api

import db.ModelService
import models.project.{Channel, Project, Version}
import models.user.User
import play.api.libs.json._

/**
  * Contains implicit JSON [[Writes]] for the Ore API.
  */
object OreWrites {

  implicit def channelWrites(implicit service: ModelService) = new Writes[Channel] {
    def writes(channel: Channel) = Json.obj("name" -> channel.name, "color" -> channel.color.hex)
  }

  implicit def projectWrites(implicit service: ModelService) = new Writes[Project] {
    def writes(project: Project) = {
      val members: List[JsObject] = (for (member <- project.members) yield {
        Json.obj(
          "name" -> JsString(member.name),
          "roles" -> JsArray(member.roles.map(r => JsString(r.roleType.title)).toSeq)
        )
      }).toList
      val category = project.category
      val rv = project.recommendedVersion
      Json.obj(
        "pluginId" -> project.pluginId,
        "createdAt" -> project.prettyDate,
        "name" -> project.name,
        "owner" -> project.ownerName,
        "description" -> project.description,
        "href" -> ('/' + project.ownerName + '/' + project.slug),
        "members" -> members,
        "channels" -> Json.toJson(project.channels.toSeq),
        "recommended" -> Json.obj("channel" -> rv.channel.name, "version" -> rv.versionString),
        "category" -> Json.obj("title" -> category.title, "icon" -> category.icon),
        "views" -> project.views,
        "downloads" -> project.downloads,
        "stars" -> project.stars
      )
    }
  }

  implicit def versionWrites(implicit service: ModelService) = new Writes[Version] {
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

  implicit def userWrites(implicit service: ModelService) = new Writes[User] {
    def writes(user: User) = {
      Json.obj(
        "id" -> user.id,
        "createdAt" -> user.prettyDate,
        "username" -> user.username,
        "roles" -> user.globalRoles.map(_.title),
        "starred" -> user.starred().map(p => p.pluginId),
        "avatarTemplate" -> user.avatarTemplate
      )
    }
  }

}
