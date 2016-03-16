package db

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

import db.OrePostgresDriver.api._
import models.auth.User
import models.author.{Dev, Team}
import models.project.{Channel, Project, Version}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Contains all queries for retrieving models from the database.
  */
object Storage {

  private lazy val config = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  /**
    * The default timeout when awaiting a query result.
    */
  val DEFAULT_TIMEOUT: Duration = Duration(10, TimeUnit.SECONDS)

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f Future to await
    * @param timeout Timeout duration
    * @tparam M Return type
    * @return Try of return type
    */
  def now[M](f: Future[M], timeout: Duration = DEFAULT_TIMEOUT): Try[M] = {
    Await.ready(f, timeout).value.get
  }

  /**
    * Returns true if the specified future has a value.
    *
    * @param f Future to check
    * @return
    */
  def isDefined(f: Future[_]): Future[Boolean] = {
    val p = Promise[Boolean]
    f.onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(m) => p.success(true)
    }
    p.future
  }

  private def q[T <: Table[_]](clazz: Class[_]): TableQuery[T] = {
    if (clazz.equals(classOf[Project])) {
      TableQuery(tag => new ProjectTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Dev])) {
      TableQuery(tag => new DevTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Team])) {
      TableQuery(tag => new TeamTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Channel])) {
      TableQuery(tag => new ChannelTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Version])) {
      TableQuery(tag => new VersionTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[User])) {
      TableQuery(tag => new UserTable(tag).asInstanceOf[T])
    } else {
      throw new Exception("No table found for class: " + clazz.toString)
    }
  }

  // Generic queries

  private def _filter[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]) = {
    q[T](clazz).filter(predicate)
  }

  private def filter[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Seq[M]] = {
    this.config.db.run(_filter[T, M](clazz, predicate).result)
  }

  private def getAll[T <: Table[M], M](clazz: Class[_]): Future[Seq[M]] = {
    val query = q[T](clazz)
    this.config.db.run(query.result)
  }

  private def optOne[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val p = Promise[Option[M]]
    filter[T, M](clazz, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(m) => p.success(m.headOption)
    }
    p.future
  }

  private def getOne[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[M] = {
    val p = Promise[M]
    optOne[T, M](clazz, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(opt) => opt match {
        case None => p.failure(new Exception("Could not retrieve required row"))
        case Some(pr) => p.success(pr)
      }
    }
    p.future
  }

  // User queries

  def getUser(username: String): Future[User] = getOne[UserTable, User](classOf[User], u => u.username === username)

  def optUser(username: String): Future[Option[User]] = optOne[UserTable, User](classOf[User], u => u.username === username)

  def createUser(user: User): Future[Unit] = {
    val users = q[UserTable](classOf[User])
    val action = DBIO.seq(users += user)
    this.config.db.run(action)
  }

  def findOrCreate(user: User): User = {
    Storage.now(optUser(user.username)) match {
      case Failure(thrown) => throw thrown
      case Success(userOpt) => userOpt match {
        case None => Storage.now(createUser(user)) match {
          case Failure(thrown) => throw thrown
          case Success(void) => user
        }
        case Some(user) => user
      }
    }
  }

  // Dev queries

  def getDevs: Future[Seq[Dev]] = getAll[DevTable, Dev](classOf[Dev])

  def optDev(name: String): Future[Option[Dev]] = optOne[DevTable, Dev](classOf[Dev], d => d.name === name)

  def optDev(id: Int): Future[Option[Dev]] = optOne[DevTable, Dev](classOf[Dev], d => d.id === id)

  def getDev(name: String): Future[Dev] = getOne[DevTable, Dev](classOf[Dev], d => d.name === name)

  def getDev(id: Int): Future[Dev] = getOne[DevTable, Dev](classOf[Dev], d => d.id === id)

  // Team queries

  def getTeams: Future[Seq[Team]] = getAll[TeamTable, Team](classOf[Team])

  def optTeam(name: String): Future[Option[Team]] = optOne[TeamTable, Team](classOf[Team], t => t.name === name)

  def optTeam(id: Int): Future[Option[Team]] = optOne[TeamTable, Team](classOf[Team], t => t.id === id)

  def getTeam(name: String): Future[Team] = getOne[TeamTable, Team](classOf[Team], t => t.name === name)

  def getTeam(id: Int): Future[Team] = getOne[TeamTable, Team](classOf[Team], t => t.id === id)

  // Project queries

  def getProjects: Future[Seq[Project]] = getAll[ProjectTable, Project](classOf[Project])

  def getProjectsBy(ownerName: String): Future[Seq[Project]] = {
    filter[ProjectTable, Project](classOf[Project], p => p.ownerName === ownerName)
  }

  def optProject(owner: String, name: String): Future[Option[Project]] = {
    optOne[ProjectTable, Project](classOf[Project], p => p.name === name && p.ownerName === owner)
  }
  
  def optProject(id: Int): Future[Option[Project]] = optOne[ProjectTable, Project](classOf[Project], p => p.id === id)

  def optProject(pluginId: String): Future[Option[Project]] = {
    optOne[ProjectTable, Project](classOf[Project], p => p.pluginId === pluginId)
  }

  def getProject(owner: String, name: String): Future[Project] = {
    getOne[ProjectTable, Project](classOf[Project], p => p.name === name && p.ownerName === owner)
  }

  def getProject(id: Int): Future[Project] = getOne[ProjectTable, Project](classOf[Project], p => p.id === id)

  def createProject(project: Project): Future[Project] = {
    // copy new vals into old project
    project.createdAt = Some(new Timestamp(new Date().getTime))
    val projects = q[ProjectTable](classOf[Project])
    val query = {
      projects returning projects.map(_.id) into {
        case (p, id) =>
          p.copy(id=Some(id))
      } += project
    }
    this.config.db.run(query)
  }

  def deleteProject(project: Project) = {
    val query = _filter[ProjectTable, Project](classOf[Project], p => p.id === project.id.get)
    val action = query.delete
    this.config.db.run(action)
  }

  def updateProjectInt(project: Project, key: ProjectTable => Rep[Int], value: Int): Future[Int] = {
    val projects = q[ProjectTable](classOf[Project])
    val query = for { p <- projects if p.id === project.id } yield key(p)
    val action = query.update(value)
    this.config.db.run(action)
  }

  def updateProjectString(project: Project, key: ProjectTable => Rep[String], value: String): Future[Int] = {
    val projects = q[ProjectTable](classOf[Project])
    val query = for { p <- projects if p.id === project.id } yield key(p)
    val action = query.update(value)
    this.config.db.run(action)
  }

  // Channel queries

  def getChannels(projectId: Int): Future[Seq[Channel]] = {
    filter[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId)
  }

  def optChannel(projectId: Int, name: String): Future[Option[Channel]] = {
    optOne[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.name === name)
  }
  
  def optChannel(id: Int): Future[Option[Channel]] = optOne[ChannelTable, Channel](classOf[Channel], c => c.id === id)

  def getChannel(projectId: Int, name: String): Future[Channel] = {
    getOne[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.name === name)
  }

  def getChannel(id: Int): Future[Channel] = getOne[ChannelTable, Channel](classOf[Channel], c => c.id === id)

  def createChannel(channel: Channel): Future[Channel] = {
    // copy new vals into old channel
    channel.createdAt = Some(new Timestamp(new Date().getTime))
    val channels = q[ChannelTable](classOf[Channel])
    val query = {
      channels returning channels.map(_.id) into {
        case (c, id) =>
          c.copy(id=Some(id))
      } += channel
    }
    this.config.db.run(query)
  }

  // Version queries

  def getAllVersions(projectId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.projectId === projectId)
  }

  def getVersions(channelId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.channelId === channelId)
  }

  def optVersion(channelId: Int, versionString: String): Future[Option[Version]] = {
    optOne[VersionTable, Version](classOf[Version], v => v.channelId === channelId && v.versionString === versionString)
  }

  def optVersion(id: Int): Future[Option[Version]] = optOne[VersionTable, Version](classOf[Version], v => v.id === id)

  def getVersion(channelId: Int, versionString: String): Future[Version] = {
    getOne[VersionTable, Version](classOf[Version], v => v.channelId === channelId && v.versionString === versionString)
  }

  def getVersion(id: Int): Future[Version] = getOne[VersionTable, Version](classOf[Version], v => v.id === id)

  def createVersion(version: Version): Future[Version] = {
    // copy new vals into old version
    version.createdAt = Some(new Timestamp(new Date().getTime))
    val versions = q[VersionTable](classOf[Version])
    val query = {
      versions returning versions.map(_.id) into {
        case (v, id) =>
          v.copy(id=Some(id))
      } += version
    }
    this.config.db.run(query)
  }

}
