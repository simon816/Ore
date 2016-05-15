package ore.rest

import javax.inject.Inject

import _root_.util.OreConfig
import _root_.util.StringUtils.prettifyDate
import db.ModelService
import db.impl.service.ProjectBase
import models.project.{Channel, Project, Version}
import models.user.User
import play.api.libs.json._

/**
  * Contains implicit JSON [[Writes]] for the Ore API.
  */
final class OreWrites @Inject()(implicit config: OreConfig, service: ModelService) {

  implicit val projects: ProjectBase = service.access(classOf[ProjectBase])

  implicit val channelWrites = new Writes[Channel] {
    def writes(channel: Channel) = Json.obj("name" -> channel.name, "color" -> channel.color.hex)
  }

  implicit val projectWrites = new Writes[Project] {
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
        "createdAt" -> prettifyDate(project.createdAt.get),
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

  implicit val versionWrites = new Writes[Version] {
    def writes(version: Version) = {
      val project = version.project
      val dependencies: List[JsObject] = version.dependencies.map { dependency =>
        Json.obj("pluginId" -> dependency.pluginId, "version" -> dependency.version)
      }
      Json.obj(
        "id" -> version.id.get,
        "createdAt" -> prettifyDate(version.createdAt.get),
        "name" -> version.versionString,
        "dependencies" -> dependencies,
        "pluginId" -> project.pluginId,
        "channel" -> Json.toJson(project.channels.get(version.channelId).get),
        "fileSize" -> version.fileSize
      )
    }
  }

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = {
      Json.obj(
        "id" -> user.id,
        "createdAt" -> prettifyDate(user.createdAt.get),
        "username" -> user.username,
        "roles" -> user.globalRoles.map(_.title),
        "starred" -> user.starred().map(p => p.pluginId),
        "avatarTemplate" -> user.avatarTemplate
      )
    }
  }

}
