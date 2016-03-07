package sql

import models.project.Channel
import slick.driver.MySQLDriver.api._

case class Storage(db: Database) {

  private val projects: TableQuery[ProjectTable] = TableQuery[ProjectTable]

  private val channels: TableQuery[ChannelTable] = TableQuery[ChannelTable]

  private val versions: TableQuery[VersionTable] = TableQuery[VersionTable]

  private val teams: TableQuery[TeamTable] = TableQuery[TeamTable]

  private val devs: TableQuery[DevTable] = TableQuery[DevTable]

  // TODO: evolutions integration
  db.run(DBIO.seq(
    (versions.schema ++ teams.schema ++ projects.schema ++ devs.schema ++ channels.schema).create,

    devs ++= Seq(
      (1, "Spongie"),
      (2, "Dev1"),
      (3, "Dev2"),
      (4, "Dev3"),
      (5, "Dev4"),
      (6, "Dev5")
    ),

    teams ++= Seq(
      (1, "SpongePowered"),
      (2, "Team1"),
      (3, "Team2"),
      (4, "Team3"),
      (5, "Team4"),
      (6, "Team5")
    ),

    versions += (1, 1, "1.0.0"),

    channels += (1, 1, "Alpha", Channel.HEX_GREEN),

    projects += (1, "org.spongepowered.ore", "Ore", "The Minecraft Plugin Repository", "Spongie", 0, 0, 0)
  ))

}

object Storage {

  private var instance: Option[Storage] = None

  def init(db: Database) = this.instance = Some(Storage(db))

  def getInstance: Storage = {
    instance match {
      case None => throw new Exception("Storage has not been initialized")
      case Some(storage) => storage
    }
  }

  def getProjects: TableQuery[ProjectTable] = getInstance.projects

  def getChannels: TableQuery[ChannelTable] = getInstance.channels

  def getVersions: TableQuery[VersionTable] = getInstance.versions

  def getTeams: TableQuery[TeamTable] = getInstance.teams

  def getDevs: TableQuery[DevTable] = getInstance.devs

}
