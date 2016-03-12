package db

import java.sql.Timestamp

import models.author.{Dev, Team}
import models.project.{Channel, Project, Version}
import slick.driver.PostgresDriver.api._

/*
 * Database schema definitions
 */

class ProjectTable(tag: Tag) extends Table[Project](tag, "projects") {

  def id                    =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt             =   column[Timestamp]("created_at")
  def pluginId              =   column[String]("plugin_id")
  def name                  =   column[String]("name")
  def description           =   column[String]("description")
  def ownerName             =   column[String]("owner_name")
  def recommendedVersionId  =   column[Int]("recommended_version_id")
  def views                 =   column[Int]("views", O.Default(0))
  def downloads             =   column[Int]("downloads", O.Default(0))
  def starred               =   column[Int]("starred", O.Default(0))

  override def * = {
    (id.?, createdAt.?, pluginId, name, description,
      ownerName, recommendedVersionId.?, views, downloads, starred) <> ((Project.apply _).tupled, Project.unapply)
  }

}

class ChannelTable(tag: Tag) extends Table[Channel](tag, "channels") {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt   =   column[Timestamp]("created_at")
  def projectId   =   column[Int]("project_id")
  def name        =   column[String]("name")
  def colorHex    =   column[String]("color_hex", O.Default(Channel.HEX_GREEN))

  override def * = (id.?, createdAt.?, projectId, name, colorHex) <> ((Channel.apply _).tupled, Channel.unapply)
}

class VersionTable(tag: Tag) extends Table[Version](tag, "versions") {

  def id              =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt       =   column[Timestamp]("created_at")
  def projectId       =   column[Int]("project_id")
  def channelId       =   column[Int]("channel_id")
  def versionString   =   column[String]("version_string")

  override def * = (id.?, createdAt.?, projectId, channelId, versionString) <> ((Version.apply _).tupled, Version.unapply)
}

class DevTable(tag: Tag) extends Table[Dev](tag, "devs") {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt   =   column[Timestamp]("created_at")
  def name        =   column[String]("name")

  override def * = (id.?, createdAt.?, name) <> (Dev.tupled, Dev.unapply)
}

class TeamTable(tag: Tag) extends Table[Team](tag, "teams") {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt   =   column[Timestamp]("created_at")
  def name        =   column[String]("name")

  override def * = (id.?, createdAt.?, name) <> (Team.tupled, Team.unapply)

}
