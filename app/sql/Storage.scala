package sql

import models.author.Author.Unknown
import models.author.{Author, Dev, Team}
import models.project.{Channel, Project, Version}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.util.{Failure, Success, Try}

object Storage {

  private val config = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  private val projects: TableQuery[ProjectTable] = TableQuery[ProjectTable]
  private val channels: TableQuery[ChannelTable] = TableQuery[ChannelTable]
  private val versions: TableQuery[VersionTable] = TableQuery[VersionTable]
  private val teams: TableQuery[TeamTable] = TableQuery[TeamTable]
  private val devs: TableQuery[DevTable] = TableQuery[DevTable]

  private def filter[T <: Table[M], M](table: TableQuery[T], predicate: T => Rep[Boolean]): Seq[M] = {
    val query = table.filter(predicate)
    this.config.db.run(query.result).value match {
      case None => Seq()
      case Some(result) => result match {
        case Failure(thrown) => throw thrown // TODO
        case Success(seq) => seq.asInstanceOf[Seq[M]]
      }
    }
  }

  private def getAll[T <: Table[M], M](table: TableQuery[T]): Seq[M] = {
    filter[T, M](table, t => true)
  }

  def getDevs: Seq[Dev] = getAll[DevTable, Dev](this.devs)

  def getDev(name: String): Option[Dev] = filter[DevTable, Dev](this.devs, d => d.name === name).headOption

  def getDev(id: Int): Option[Dev] = filter[DevTable, Dev](this.devs, d => d.id === id).headOption

  def getTeams: Seq[Team] = getAll[TeamTable, Team](this.teams)

  def getTeam(name: String): Option[Team] = filter[TeamTable, Team](this.teams, t => t.name === name).headOption

  def getTeam(id: Int): Option[Team] = filter[TeamTable, Team](this.teams, t => t.id === id).headOption

  def getAuthor(name: String): Author = getDev(name).getOrElse(getTeam(name).getOrElse(Unknown(name=name)))

  def getProjects: Seq[Project] = getAll[ProjectTable, Project](this.projects)

  def getProjectsBy(ownerName: String): Seq[Project] = {
    filter[ProjectTable, Project](this.projects, p => p.ownerName === ownerName)
  }

  def getProject(name: String, owner: String): Option[Project] = {
    filter[ProjectTable, Project](this.projects, p => p.name === name && p.ownerName === owner).headOption
  }
  
  def getProject(id: Int): Option[Project] = filter[ProjectTable, Project](this.projects, p => p.id === id).headOption

  def createProject(project: Project): Try[Project] = {
    // copy new vals into old project
    val query = {
      this.projects returning this.projects.map(p => (p.id, p.createdAt, p.views, p.downloads, p.starred)) into {
        case (p, (id, createdAt, views, downs, stars)) =>
          p.copy(id=id, createdAt=createdAt, views=views, downloads=downs, starred=stars)
      } += project
    }

    Try {
      this.config.db.run(query).value match {
        case None => throw new Exception("Slick returned None type for Project insert result.")
        case Some(result) => result match {
          case Failure(thrown) => throw thrown
          case Success(p) => p
        }
      }
    }
  }

  def getChannels(projectId: Int): Seq[Channel] = {
    filter[ChannelTable, Channel](this.channels, c => c.projectId === projectId)
  }

  def getChannel(projectId: Int, name: String): Option[Channel] = {
    filter[ChannelTable, Channel](this.channels, c => c.projectId === projectId && c.name === name).headOption
  }
  
  def getChannel(id: Int): Option[Channel] = {
    filter[ChannelTable, Channel](this.channels, c => c.id === id).headOption
  }

  def createChannel(channel: Channel): Try[Channel] = {
    // copy new vals into old channel
    val query = this.channels returning this.channels.map(c => (c.id, c.createdAt)) into {
      case (c, (id, createdAt)) =>
        c.copy(id=id, createdAt=createdAt)
    } += channel

    Try {
      this.config.db.run(query).value match {
        case None => throw new Exception("Slick returned None type for Channel insert result.")
        case Some(result) => result match {
          case Failure(thrown) => throw thrown
          case Success(c) => c
        }
      }
    }
  }

  def getAllVersions(projectId: Int): Seq[Version] = {
    filter[VersionTable, Version](this.versions, v => v.projectId === projectId)
  }

  def getVersions(channelId: Int): Seq[Version] = {
    filter[VersionTable, Version](this.versions, v => v.channelId === channelId)
  }

  def getVersion(channelId: Int, versionString: String): Option[Version] = {
    filter[VersionTable, Version](this.versions,
      v => v.channelId === channelId && v.versionString === versionString).headOption
  }

  def getVersion(id: Int): Option[Version] = {
    filter[VersionTable, Version](this.versions, v => v.id === id).headOption
  }

  def createVersion(version: Version): Try[Version] = {
    // copy new vals into old version
    val query = this.versions returning this.versions.map(v => (v.id, v.createdAt)) into {
      case (v, (id, createdAt)) =>
        v.copy(id=id, createdAt=createdAt)
    } += version

    Try {
      this.config.db.run(query).value match {
        case None => throw new Exception("Slick returned None type for Version insert result.")
        case Some(result) => result match {
          case Failure(thrown) => throw thrown
          case Success(c) => c
        }
      }
    }
  }

}
