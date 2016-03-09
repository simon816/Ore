package sql

import models.author.{Dev, Team}
import models.project.{Channel, Project, Version}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}

object Storage {

  private val config = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  private val projects: TableQuery[ProjectTable] = TableQuery[ProjectTable]
  private val channels: TableQuery[ChannelTable] = TableQuery[ChannelTable]
  private val versions: TableQuery[VersionTable] = TableQuery[VersionTable]
  private val teams: TableQuery[TeamTable] = TableQuery[TeamTable]
  private val devs: TableQuery[DevTable] = TableQuery[DevTable]

  private def filter[T <: Table[M], M](table: TableQuery[T], predicate: T => Rep[Boolean]): Future[Seq[M]] = {
    val query = table.filter(predicate)
    this.config.db.run(query.result)
  }

  private def getAll[T <: Table[M], M](table: TableQuery[T]): Future[Seq[M]] = {
    filter[T, M](table, t => true)
  }

  private def optOne[T <: Table[M], M](table: TableQuery[T], predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val p = Promise[Option[M]]
    filter(table, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(m) => p.success(m.headOption)
    }
    p.future
  }

  private def getOne[T <: Table[M], M](table: TableQuery[T], predicate: T => Rep[Boolean]): Future[M] = {
    val p = Promise[M]
    optOne(table, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(opt) => opt match {
        case None => p.failure(new Exception("Could not retrieve required row"))
        case Some(pr) => p.success(pr)
      }
    }
    p.future
  }

  def getDevs: Future[Seq[Dev]] = getAll[DevTable, Dev](this.devs)

  def optDev(name: String): Future[Option[Dev]] = optOne[DevTable, Dev](this.devs, d => d.name === name)

  def optDev(id: Int): Future[Option[Dev]] = optOne[DevTable, Dev](this.devs, d => d.id === id)

  def getDev(name: String): Future[Dev] = getOne[DevTable, Dev](this.devs, d => d.name === name)

  def getDev(id: Int): Future[Dev] = getOne[DevTable, Dev](this.devs, d => d.id === id)

  def getTeams: Future[Seq[Team]] = getAll[TeamTable, Team](this.teams)

  def optTeam(name: String): Future[Option[Team]] = optOne[TeamTable, Team](this.teams, t => t.name === name)

  def optTeam(id: Int): Future[Option[Team]] = optOne[TeamTable, Team](this.teams, t => t.id === id)

  def getTeam(name: String): Future[Team] = getOne[TeamTable, Team](this.teams, t => t.name === name)

  def getTeam(id: Int): Future[Team] = getOne[TeamTable, Team](this.teams, t => t.id === id)

  def getProjects: Future[Seq[Project]] = getAll[ProjectTable, Project](this.projects)

  def getProjectsBy(ownerName: String): Future[Seq[Project]] = {
    filter[ProjectTable, Project](this.projects, p => p.ownerName === ownerName)
  }

  def optProject(name: String, owner: String): Future[Option[Project]] = {
    optOne[ProjectTable, Project](this.projects, p => p.name === name && p.ownerName === owner)
  }
  
  def optProject(id: Int): Future[Option[Project]] = optOne[ProjectTable, Project](this.projects, p => p.id === id)

  def getProject(name: String, owner: String): Future[Project] = {
    getOne[ProjectTable, Project](this.projects, p => p.name === name && p.ownerName === owner)
  }

  def getProject(id: Int): Future[Project] = getOne[ProjectTable, Project](this.projects, p => p.id === id)

  def createProject(project: Project): Future[Project] = {
    // copy new vals into old project
    val query = {
      this.projects returning this.projects.map(p => (p.id, p.createdAt, p.views, p.downloads, p.starred)) into {
        case (p, (id, createdAt, views, downs, stars)) =>
          p.copy(id=id, createdAt=createdAt, views=views, downloads=downs, starred=stars)
      } += project
    }
    this.config.db.run(query)
  }

  def getChannels(projectId: Int): Future[Seq[Channel]] = {
    filter[ChannelTable, Channel](this.channels, c => c.projectId === projectId)
  }

  def optChannel(projectId: Int, name: String): Future[Option[Channel]] = {
    optOne[ChannelTable, Channel](this.channels, c => c.projectId === projectId && c.name === name)
  }
  
  def optChannel(id: Int): Future[Option[Channel]] = {
    optOne[ChannelTable, Channel](this.channels, c => c.id === id)
  }

  def getChannel(projectId: Int, name: String): Future[Channel] = {
    getOne[ChannelTable, Channel](this.channels, c => c.projectId === projectId && c.name === name)
  }

  def getChannel(id: Int): Future[Channel] = {
    getOne[ChannelTable, Channel](this.channels, c => c.id === id)
  }

  def createChannel(channel: Channel): Future[Channel] = {
    // copy new vals into old channel
    val query = this.channels returning this.channels.map(c => (c.id, c.createdAt)) into {
      case (c, (id, createdAt)) =>
        c.copy(id=id, createdAt=createdAt)
    } += channel
    this.config.db.run(query)
  }

  def getAllVersions(projectId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](this.versions, v => v.projectId === projectId)
  }

  def getVersions(channelId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](this.versions, v => v.channelId === channelId)
  }

  def optVersion(channelId: Int, versionString: String): Future[Option[Version]] = {
    optOne[VersionTable, Version](this.versions,
      v => v.channelId === channelId && v.versionString === versionString)
  }

  def optVersion(id: Int): Future[Option[Version]] = {
    optOne[VersionTable, Version](this.versions, v => v.id === id)
  }

  def getVersion(channelId: Int, versionString: String): Future[Version] = {
    getOne[VersionTable, Version](this.versions, v => v.channelId === channelId && v.versionString === versionString)
  }

  def getVersion(id: Int): Future[Version] = getOne[VersionTable, Version](this.versions, v => v.id === id)

  def createVersion(version: Version): Future[Version] = {
    // copy new vals into old version
    val query = this.versions returning this.versions.map(v => (v.id, v.createdAt)) into {
      case (v, (id, createdAt)) =>
        v.copy(id=id, createdAt=createdAt)
    } += version
    this.config.db.run(query)
  }

}
