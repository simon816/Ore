package sql

import slick.driver.MySQLDriver.api._

object Storage {

  def getProjects: TableQuery[ProjectTable] = TableQuery[ProjectTable]

  def getChannels: TableQuery[ChannelTable] = TableQuery[ChannelTable]

  def getVersions: TableQuery[VersionTable] = TableQuery[VersionTable]

  def getTeams: TableQuery[TeamTable] = TableQuery[TeamTable]

  def getDevs: TableQuery[DevTable] = TableQuery[DevTable]

}
