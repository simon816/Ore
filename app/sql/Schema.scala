package sql

import slick.driver.MySQLDriver.api._

class Projects(tag: Tag) extends Table[(Int, String, String, String, String, Int, Int, Int)](tag, "projects") {
  def id = column[Int]("id", O.PrimaryKey)
  def pluginId = column[String]("plugin_id")
  def name = column[String]("name")
  def description = column[String]("description")
  def ownerName = column[String]("owner_name")
  def views = column[Int]("views")
  def downloads = column[Int]("downloads")
  def starred = column[Int]("starred")
  override def * = (id, pluginId, name, description, ownerName, views, downloads, starred)
}

class Channels(tag: Tag) extends Table[(Int, Int, String, String)](tag, "channels") {
  def id = column[Int]("id", O.PrimaryKey)
  def projectId = column[Int]("project_id")
  def name = column[String]("name")
  def colorHex = column[String]("color_hex")
  override def * = (id, projectId, name, colorHex)
}

class Versions(tag: Tag) extends Table[(Int, Int, String)](tag, "versions") {
  def id = column[Int]("id", O.PrimaryKey)
  def channelId = column[Int]("channel_id")
  def versionString = column[String]("version_string")
  override def * = (id, channelId, versionString)
}

class Devs(tag: Tag) extends Table[(Int, String)](tag, "devs") {
  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  override def * = (id, name)
}

class Teams(tag: Tag) extends Table[(Int, String)](tag, "teams") {
  def id = column[Int]("id", O.PrimaryKey)
  def name = column[String]("name")
  override def * = (id, name)
}
