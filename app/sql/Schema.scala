package sql

import java.sql.Timestamp

import models.project.Channel
import slick.driver.MySQLDriver.api._

class ProjectTable(tag: Tag) extends Table[(Int, Timestamp, String, String, String, String, Int, Int, Int)](tag, "projects") {
  def id = column[Int]("id", O.PrimaryKey)
  def createdAt = column[Timestamp]("created_at")
  def pluginId = column[String]("plugin_id")
  def name = column[String]("name")
  def description = column[String]("description")
  def ownerName = column[String]("owner_name")
  def views = column[Int]("views", O.Default(0))
  def downloads = column[Int]("downloads", O.Default(0))
  def starred = column[Int]("starred", O.Default(0))
  override def * = (id, createdAt, pluginId, name, description, ownerName, views, downloads, starred)
}

class ChannelTable(tag: Tag) extends Table[(Int, Timestamp, Int, String, String)](tag, "channels") {
  def id = column[Int]("id", O.PrimaryKey)
  def createdAt = column[Timestamp]("created_at")
  def projectId = column[Int]("project_id")
  def name = column[String]("name")
  def colorHex = column[String]("color_hex", O.Default(Channel.HEX_GREEN))
  override def * = (id, createdAt, projectId, name, colorHex)
}

class VersionTable(tag: Tag) extends Table[(Int, Timestamp, Int, String)](tag, "versions") {
  def id = column[Int]("id", O.PrimaryKey)
  def createdAt = column[Timestamp]("created_at")
  def channelId = column[Int]("channel_id")
  def versionString = column[String]("version_string")
  override def * = (id, createdAt, channelId, versionString)
}

class DevTable(tag: Tag) extends Table[(Int, Timestamp, String)](tag, "devs") {
  def id = column[Int]("id", O.PrimaryKey)
  def createdAt = column[Timestamp]("created_at")
  def name = column[String]("name")
  override def * = (id, createdAt, name)
}

class TeamTable(tag: Tag) extends Table[(Int, Timestamp, String)](tag, "teams") {
  def id = column[Int]("id", O.PrimaryKey)
  def createdAt = column[Timestamp]("created_at")
  def name = column[String]("name")
  override def * = (id, createdAt, name)
}
