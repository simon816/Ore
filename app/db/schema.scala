package db

import db.OrePostgresDriver.api._
import db.orm.{ModelTable, NamedModelTable}
import models.project.{Channel, Page, Project, Version}
import models.user.{ProjectRole, User}
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category

/*
 * Database schema definitions. Changes must be first applied as an evolutions
 * SQL script in "conf/evolutions/default", then here, then in the associated
 * model.
 */

class ProjectTable(tag: Tag) extends NamedModelTable[Project](tag, "projects") {

  def pluginId              =   column[String]("plugin_id")
  def name                  =   column[String]("name")
  def slug                  =   column[String]("slug")
  def ownerName             =   column[String]("owner_name")
  def ownerId               =   column[Int]("owner_id")
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

  override def modelName = this.name

  override def * = (id.?, createdAt.?, pluginId, name, slug, ownerName, ownerId, homepage.?, recommendedVersionId.?,
                    category, views, downloads, stars, issues.?, source.?, description.?, topicId.?,
                    postId.?) <> ((Project.apply _).tupled, Project.unapply)

}

class ProjectViewsTable(tag: Tag) extends Table[(Option[Int], Option[String],
                                                 Option[Int], Int)](tag, "project_views") {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cookie      =   column[String]("cookie")
  def userId      =   column[Int]("user_id")
  def projectId   =   column[Int]("project_id")

  override def * = (id.?, cookie.?, userId.?, projectId)

}

class ProjectStarsTable(tag: Tag) extends Table[(Int, Int)](tag, "project_stars") {

  def userId      =   column[Int]("user_id")
  def projectId   =   column[Int]("project_id")

  override def * = (userId, projectId)

}

class PageTable(tag: Tag) extends NamedModelTable[Page](tag, "pages") {

  def projectId     =   column[Int]("project_id")
  def name          =   column[String]("name")
  def slug          =   column[String]("slug")
  def contents      =   column[String]("contents")
  def isDeletable   =   column[Boolean]("is_deletable")

  override def modelName = this.name

  override def * = (id.?, createdAt.?, projectId,
                    name, slug, contents, isDeletable) <> ((Page.apply _).tupled, Page.unapply)

}

class ChannelTable(tag: Tag) extends NamedModelTable[Channel](tag, "channels") {

  def name        =   column[String]("name")
  def color       =   column[Color]("color")
  def projectId   =   column[Int]("project_id")

  override def modelName = this.name

  override def * = (id.?, createdAt.?, name, color, projectId) <> ((Channel.apply _).tupled, Channel.unapply)
}

class VersionTable(tag: Tag) extends NamedModelTable[Version](tag, "versions") {

  def versionString   =   column[String]("version_string")
  def dependencies    =   column[List[String]]("dependencies")
  def description     =   column[String]("description")
  def assets          =   column[String]("assets")
  def downloads       =   column[Int]("downloads")
  def projectId       =   column[Int]("project_id")
  def channelId       =   column[Int]("channel_id")
  def fileSize        =   column[Long]("file_size")

  override def modelName = this.versionString

  override def * = (id.?, createdAt.?, versionString, dependencies, description.?,
                    assets.?, downloads, projectId, channelId,
                    fileSize) <> ((Version.apply _).tupled, Version.unapply)
}

class VersionDownloadsTable(tag: Tag) extends Table[(Option[Int], Option[String],
                                                     Option[Int], Int)](tag, "version_downloads") {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cookie      =   column[String]("cookie")
  def userId      =   column[Int]("user_id")
  def versionId   =   column[Int]("version_id")

  override def * = (id.?, cookie.?, userId.?, versionId)

}

class UserTable(tag: Tag) extends NamedModelTable[User](tag, "users") {

  override def id   =   column[Int]("id", O.PrimaryKey)
  def name          =   column[String]("name")
  def username      =   column[String]("username")
  def email         =   column[String]("email")
  def tagline       =   column[String]("tagline")
  def globalRoles   =   column[List[Int]]("global_roles")

  override def modelName = this.username

  override def * = (id.?, createdAt.?, name.?, username, email.?, tagline.?,
                    globalRoles) <> ((User.apply _).tupled, User.unapply)

}

class UserProjectRolesTable(tag: Tag) extends ModelTable[ProjectRole](tag, "user_project_roles") {

  def userId      =   column[Int]("user_id")
  def roleType    =   column[RoleType]("role_type")
  def projectId   =   column[Int]("project_id")

  override def * = (id.?, createdAt.?, userId, roleType, projectId) <> ((ProjectRole.apply _).tupled, ProjectRole.unapply)

}
