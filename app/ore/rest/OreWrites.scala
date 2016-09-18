package ore.rest

import javax.inject.Inject

import _root_.util.OreConfig
import _root_.util.StringUtils.prettifyDate
import db.ModelService
import db.impl.access.ProjectBase
import models.project.{Channel, Project, Version}
import models.user.User
import ore.project.ProjectMember
import play.api.libs.json.Json.{obj, toJson}
import play.api.libs.json._

/**
  * Contains implicit JSON [[Writes]] for the Ore API.
  */
final class OreWrites @Inject()(implicit config: OreConfig, service: ModelService) {

  implicit val projects: ProjectBase = this.service.access(classOf[ProjectBase])

  implicit val channelWrites = new Writes[Channel] {
    def writes(channel: Channel) = obj("name" -> channel.name, "color" -> channel.color.hex)
  }

  implicit val memberWrites = new Writes[ProjectMember] {
    def writes(member: ProjectMember) = {
      obj(
        "userId"    ->  member.user.id,
        "name"      ->  member.username,
        "roles"     ->  JsArray(member.roles.map(r => JsString(r.roleType.title)).toSeq),
        "headRole"  ->  member.headRole.roleType.title
      )
    }
  }

  implicit val versionWrites = new Writes[Version] {
    def writes(version: Version) = {
      val project = version.project
      val dependencies: List[JsObject] = version.dependencies.map { dependency =>
        obj("pluginId" -> dependency.pluginId, "version" -> dependency.version)
      }

      obj(
        "id"            ->  version.id.get,
        "createdAt"     ->  prettifyDate(version.createdAt.get),
        "name"          ->  version.versionString,
        "dependencies"  ->  dependencies,
        "pluginId"      ->  project.pluginId,
        "channel"       ->  toJson(project.channels.get(version.channelId).get),
        "fileSize"      ->  version.fileSize
      )
    }
  }

  implicit val projectWrites = new Writes[Project] {
    def writes(project: Project) = {
      val category = project.category
      obj(
        "pluginId"      ->  project.pluginId,
        "createdAt"     ->  prettifyDate(project.createdAt.get),
        "name"          ->  project.name,
        "owner"         ->  project.ownerName,
        "description"   ->  project.description,
        "href"          ->  ('/' + project.ownerName + '/' + project.slug),
        "members"       ->  project.memberships.members, // TODO: filter out members who have not accepted the invite
        "channels"      ->  toJson(project.channels.toSeq),
        "recommended"   ->  toJson(project.recommendedVersion),
        "category"      ->  obj("title" -> category.title, "icon" -> category.icon),
        "views"         ->  project.views,
        "downloads"     ->  project.downloads,
        "stars"         ->  project.stars
      )
    }
  }

  implicit val userWrites = new Writes[User] {
    def writes(user: User) = {
      obj(
        "id"              ->  user.id,
        "createdAt"       ->  prettifyDate(user.createdAt.get),
        "username"        ->  user.username,
        "roles"           ->  user.globalRoles.map(_.title),
        "starred"         ->  user.starred().map(p => p.pluginId),
        "avatarTemplate"  ->  user.avatarTemplate,
        "projects"        ->  user.projects.all
      )
    }
  }

}
