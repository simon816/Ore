package db.impl

import java.sql.Timestamp

import db.impl.pg.OrePostgresDriver.api._
import db.{AssociativeTable, ModelTable}
import models.project._
import models.statistic.{ProjectView, VersionDownload}
import models.user.{Notification, ProjectRole, User}
import models.user.{Organization, ProjectRole, User}
import ore.Colors.Color
import ore.notification.NotificationTypes.NotificationType
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

/*
 * Database schema definitions. Changes must be first applied as an evolutions
 * SQL script in "conf/evolutions/default", then here, then in the associated
 * model.
 */

class ProjectTable(tag: Tag) extends ModelTable[Project](tag, "projects") {

  def pluginId              =   column[String]("plugin_id")
  def name                  =   column[String]("name")
  def slug                  =   column[String]("slug")
  def ownerName             =   column[String]("owner_name")
  def userId                =   column[Int]("owner_id")
  def organizationId        =   column[Int]("organization_id")
  def homepage              =   column[String]("homepage")
  def recommendedVersionId  =   column[Int]("recommended_version_id")
  def category              =   column[Category]("category")
  def views                 =   column[Int]("views", O.Default(0))
  def downloads             =   column[Int]("downloads", O.Default(0))
  def stars                 =   column[Int]("stars", O.Default(0))
  def issues                =   column[String]("issues")
  def source                =   column[String]("source")
  def description           =   column[String]("description")
  def topicId               =   column[Int]("topic_id")
  def postId                =   column[Int]("post_id")
  def isVisible             =   column[Boolean]("is_visible")
  def lastUpdated           =   column[Timestamp]("last_updated")

  override def * = (id.?, createdAt.?, pluginId, ownerName, userId, homepage.?, name, slug, recommendedVersionId.?,
                    category, views, downloads, stars, issues.?, source.?, description.?, topicId.?, postId.?,
                    isVisible, lastUpdated) <> ((Project.apply _).tupled, Project.unapply)

}

class ProjectWatchersTable(tag: Tag)
  extends AssociativeTable(tag, "project_watchers", classOf[Project], classOf[User]) {

  def projectId = column[Int]("project_id")
  def userId = column[Int]("user_id")

  override def * = (projectId, userId)

}

class ProjectViewsTable(tag: Tag) extends StatTable[ProjectView](tag, "project_views", "project_id") {
  override def * = (id.?, createdAt.?, modelId, address, cookie,
                    userId.?) <> ((ProjectView.apply _).tupled, ProjectView.unapply)
}

class ProjectStarsTable(tag: Tag) extends Table[(Int, Int)](tag, "project_stars") {

  def userId      =   column[Int]("user_id")
  def projectId   =   column[Int]("project_id")

  override def * = (userId, projectId)

}

class PageTable(tag: Tag) extends ModelTable[Page](tag, "pages") {

  def projectId     =   column[Int]("project_id")
  def name          =   column[String]("name")
  def slug          =   column[String]("slug")
  def contents      =   column[String]("contents")
  def isDeletable   =   column[Boolean]("is_deletable")

  override def * = (id.?, createdAt.?, projectId, name, slug, isDeletable,
                    contents) <> ((Page.apply _).tupled, Page.unapply)

}

class ChannelTable(tag: Tag) extends ModelTable[Channel](tag, "channels") {

  def name        =   column[String]("name")
  def color       =   column[Color]("color")
  def projectId   =   column[Int]("project_id")

  override def * = (id.?, createdAt.?, projectId, name, color) <> ((Channel.apply _).tupled, Channel.unapply)
}

class VersionTable(tag: Tag) extends ModelTable[Version](tag, "versions") {

  def versionString   =   column[String]("version_string")
  def mcversion       =   column[String]("mcversion")
  def dependencies    =   column[List[String]]("dependencies")
  def description     =   column[String]("description")
  def assets          =   column[String]("assets")
  def downloads       =   column[Int]("downloads")
  def projectId       =   column[Int]("project_id")
  def channelId       =   column[Int]("channel_id")
  def fileSize        =   column[Long]("file_size")
  def hash            =   column[String]("hash")
  def isReviewed      =   column[Boolean]("is_reviewed")
  def fileName        =   column[String]("file_name")

  override def * = (id.?, createdAt.?, projectId, versionString, mcversion.?, dependencies, assets.?, channelId,
                    fileSize, hash, description.?, downloads, isReviewed, fileName) <> ((Version.apply _).tupled,
                    Version.unapply)
}

class VersionDownloadsTable(tag: Tag) extends StatTable[VersionDownload](tag, "version_downloads", "version_id") {
  override def * = (id.?, createdAt.?, modelId, address, cookie, userId.?) <> ((VersionDownload.apply _).tupled,
                    VersionDownload.unapply)
}

class UserTable(tag: Tag) extends ModelTable[User](tag, "users") {

  // Override to remove auto increment
  override def id   =   column[Int]("id", O.PrimaryKey)

  def name          =   column[String]("name")
  def username      =   column[String]("username")
  def email         =   column[String]("email")
  def tagline       =   column[String]("tagline")
  def globalRoles   =   column[List[RoleType]]("global_roles")
  def joinDate      =   column[Timestamp]("join_date")
  def avatarUrl     =   column[String]("avatar_url")

  override def * = (id.?, createdAt.?, name.?, username, email.?, tagline.?,
                    globalRoles, joinDate.?, avatarUrl.?) <> ((User.apply _).tupled, User.unapply)

}

class OrganizationTable(tag: Tag) extends ModelTable[Organization](tag, "organizations") {

  override def id   =   column[Int]("id", O.PrimaryKey)
  def name          =   column[String]("name")
  def password      =   column[String]("password")
  def ownerId       =   column[Int]("owner_id")
  def avatarUrl     =   column[String]("avatar_url")
  def tagline       =   column[String]("tagline")
  def globalRoles   =   column[List[RoleType]]("global_roles")

  override def * = (id.?, createdAt.?, name, password, ownerId, avatarUrl.?, tagline.?,
                    globalRoles) <> ((Organization.apply _).tupled, Organization.unapply)

}

class OrganizationMembersTable(tag: Tag) extends Table[(Int, Int)](tag, "organization_members") {

  def userId          =   column[Int]("user_id")
  def organizationId  =   column[Int]("organization_id")

  override def * = (userId, organizationId)

}

class ProjectRoleTable(tag: Tag) extends ModelTable[ProjectRole](tag, "user_project_roles") {

  def userId      =   column[Int]("user_id")
  def roleType    =   column[RoleType]("role_type")
  def projectId   =   column[Int]("project_id")
  def isAccepted  =   column[Boolean]("is_accepted")

  override def * = (id.?, createdAt.?, userId, projectId, roleType, isAccepted) <> ((ProjectRole.apply _).tupled,
                    ProjectRole.unapply)

}

class NotificationTable(tag: Tag) extends ModelTable[Notification](tag, "notifications") {

  def userId            =   column[Int]("user_id")
  def originId          =   column[Int]("origin_id")
  def notificationType  =   column[NotificationType]("notification_type")
  def message           =   column[String]("message")
  def action            =   column[String]("action")
  def read              =   column[Boolean]("read")

  override def * = (id.?, createdAt.?, userId, originId, notificationType, message, action.?,
                    read) <> ((Notification.apply _).tupled, Notification.unapply)

}

class FlagTable(tag: Tag) extends ModelTable[Flag](tag, "flags") {

  def projectId = column[Int]("project_id")
  def userId = column[Int]("user_id")
  def reason = column[FlagReason]("reason")
  def isResolved = column[Boolean]("is_resolved")

  override def * = (id.?, createdAt.?, projectId, userId, reason, isResolved) <> ((Flag.apply _).tupled, Flag.unapply)

}
