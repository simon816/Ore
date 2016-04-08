package db

import java.util.concurrent.TimeUnit

import db.OrePostgresDriver.api._
import models.auth.User
import models.author.Team
import models.project.{Channel, Project, Page, Version}
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
  private lazy val projectViews = TableQuery[ProjectViewsTable]
  private lazy val versionDownloads = TableQuery[VersionDownloadsTable]
  private lazy val starredProjects = TableQuery[StarredProjectsTable]

  /**
    * The default timeout when awaiting a query result.
    */
  val DEFAULT_TIMEOUT: Duration = Duration(10, TimeUnit.SECONDS)

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f        Future to await
    * @param timeout  Timeout duration
    * @tparam M       Return type
    * @return         Try of return type
    */
  def now[M](f: Future[M], timeout: Duration = DEFAULT_TIMEOUT): Try[M] = {
    Await.ready(f, timeout).value.get
  }

  /**
    * Returns true if the specified future has a value.
    *
    * @param f  Future to check
    * @return   Future
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
    // Table mappings
    if (clazz.equals(classOf[Project])) {
      TableQuery(tag => new ProjectTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Team])) {
      TableQuery(tag => new TeamTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Channel])) {
      TableQuery(tag => new ChannelTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Version])) {
      TableQuery(tag => new VersionTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[User])) {
      TableQuery(tag => new UserTable(tag).asInstanceOf[T])
    } else if (clazz.equals(classOf[Page])) {
      TableQuery(tag => new PagesTable(tag).asInstanceOf[T])
    } else {
      throw new Exception("No table found for class: " + clazz.toString)
    }
  }

  // Generic queries

  private def _filter[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]) = {
    // Raw filter query
    q[T](clazz).filter(predicate)
  }

  private def filter[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Seq[M]] = {
    // Filter action
    this.config.db.run(_filter[T, M](clazz, predicate).result)
  }

  private def all[T <: Table[M], M](clazz: Class[_]): Future[Seq[M]] = {
    val query = q[T](clazz)
    this.config.db.run(query.result)
  }

  private def get[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val p = Promise[Option[M]]
    filter[T, M](clazz, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(m) => p.success(m.headOption)
    }
    p.future
  }

  private def getOne[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[M] = {
    // TODO: Remove
    val p = Promise[M]
    get[T, M](clazz, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(opt) => opt match {
        case None => p.failure(new Exception("Could not retrieve required row"))
        case Some(pr) => p.success(pr)
      }
    }
    p.future
  }

  // Project queries

  def projects(categories: Array[Int] = null, limit: Int = -1, offset: Int = -1): Future[Seq[Project]] = {
    var query: Query[ProjectTable, Project, Seq] = q[ProjectTable](classOf[Project])
    if (categories != null) {
      query = for {
        project <- query
        if project.categoryId inSetBind categories
      } yield project
    }
    if (offset > -1) {
      query = query.drop(offset)
    }
    if (limit > -1) {
      query = query.take(limit)
    }
    this.config.db.run(query.result)
  }

  def projectsBy(ownerName: String): Future[Seq[Project]] = {
    filter[ProjectTable, Project](classOf[Project], p => p.ownerName === ownerName)
  }

  def projectWithName(owner: String, name: String): Future[Option[Project]] = {
    get[ProjectTable, Project](classOf[Project], p => p.name === name && p.ownerName === owner)
  }

  def projectWithPluginId(pluginId: String): Future[Option[Project]] = {
    get[ProjectTable, Project](classOf[Project], p => p.pluginId === pluginId)
  }

  def projectWithSlug(owner: String, slug: String): Future[Option[Project]] = {
    get[ProjectTable, Project](classOf[Project], p => p.ownerName === owner && p.slug.toLowerCase === slug.toLowerCase)
  }

  def projectWithId(id: Int): Future[Option[Project]] = get[ProjectTable, Project](classOf[Project], p => p.id === id)

  def createProject(project: Project): Future[Project] = {
    // copy new vals into old project
    project.onCreate()
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
    val query = for { p <- projects if p.id === project.id.get } yield key(p)
    val action = query.update(value)
    this.config.db.run(action)
  }

  def updateProjectString(project: Project, key: ProjectTable => Rep[String], value: String): Future[Int] = {
    val projects = q[ProjectTable](classOf[Project])
    val query = for { p <- projects if p.id === project.id.get } yield key(p)
    val action = query.update(value)
    this.config.db.run(action)
  }

  def hasProjectBeenViewedBy(project: Project, cookie: String): Future[Boolean] = {
    val query = this.projectViews.filter(pv => pv.projectId === project.id.get && pv.cookie === cookie).size > 0
    this.config.db.run(query.result)
  }

  def setProjectViewedBy(project: Project, cookie: String) = {
    val query = this.projectViews += (None, Some(cookie), None, project.id.get)
    this.config.db.run(query)
  }

  def hasProjectBeenViewedBy(project: Project, user: User): Future[Boolean] = {
    val query = this.projectViews.filter(pv => pv.projectId === project.id.get && pv.userId === user.externalId).size > 0
    this.config.db.run(query.result)
  }

  def setProjectViewedBy(project: Project, user: User) = {
    val query = this.projectViews += (None, None, Some(user.externalId), project.id.get)
    this.config.db.run(query)
  }

  def isProjectStarredBy(project: Project, user: User): Future[Boolean] = {
    val query = this.starredProjects.filter(sp => sp.userId === user.externalId
      && sp.projectId === project.id.get).size > 0
    this.config.db.run(query.result)
  }

  def starProjectFor(project: Project, user: User): Future[Int] = {
    val query = this.starredProjects += (user.externalId, project.id.get)
    this.config.db.run(query)
  }

  def unstarProjectFor(project: Project, user: User): Future[Int] = {
    val query = this.starredProjects.filter(sp => sp.userId === user.externalId
      && sp.projectId === project.id.get).delete
    this.config.db.run(query)
  }

  // User queries

  def userWithName(username: String): Future[Option[User]] = get[UserTable, User](classOf[User], u => u.username === username)

  def createUser(user: User): Future[Unit] = {
    val users = q[UserTable](classOf[User])
    val action = DBIO.seq(users += user)
    this.config.db.run(action)
  }

  def getOrCreateUser(user: User): User = {
    Storage.now(userWithName(user.username)) match {
      case Failure(thrown) => throw thrown
      case Success(userOpt) => userOpt match {
        case None => Storage.now(createUser(user)) match {
          case Failure(thrown) => throw thrown
          case Success(void) => user
        }
        case Some(u) => u
      }
    }
  }

  // Channel queries

  def channelsInProject(projectId: Int): Future[Seq[Channel]] = {
    filter[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId)
  }

  def channelWithName(projectId: Int, name: String): Future[Option[Channel]] = {
    get[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.name.toLowerCase === name.toLowerCase)
  }

  def channelWithColor(projectId: Int, colorId: Int): Future[Option[Channel]] = {
    get[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.colorId === colorId)
  }

  def channelWithId(id: Int): Future[Option[Channel]] = get[ChannelTable, Channel](classOf[Channel], c => c.id === id)

  def createChannel(channel: Channel): Future[Channel] = {
    // copy new vals into old channel
    channel.onCreate()
    val channels = q[ChannelTable](classOf[Channel])
    val query = {
      channels returning channels.map(_.id) into {
        case (c, id) =>
          c.copy(id=Some(id))
      } += channel
    }
    this.config.db.run(query)
  }

  def deleteChannel(channel: Channel) = {
    val query = _filter[ChannelTable, Channel](classOf[Channel], c => c.id === channel.id.get)
    val action = query.delete
    this.config.db.run(action)
  }

  def updateChannelString(channel: Channel, key: ChannelTable => Rep[String], value: String): Future[Int] = {
    val channels = q[ChannelTable](classOf[Channel])
    val query = for { c <- channels if c.id === channel.id.get } yield key(c)
    val action = query.update(value)
    this.config.db.run(action)
  }

  def updateChannelInt(channel: Channel, key: ChannelTable => Rep[Int], value: Int): Future[Int] = {
    val channels = q[ChannelTable](classOf[Channel])
    val query = for { c <- channels if c.id === channel.id.get } yield key(c)
    val action = query.update(value)
    this.config.db.run(action)
  }

  // Version queries

  def versionsInProject(projectId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.projectId === projectId)
  }

  def versionsInChannel(channelId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.channelId === channelId)
  }

  def versionsInChannels(projectId: Int, channelIds: Seq[Int]): Future[Seq[Version]] = {
    val query = for {
      version <- q[VersionTable](classOf[Version])
      if version.projectId === projectId
      if version.channelId inSetBind channelIds
    } yield version
    this.config.db.run(query.result)
  }

  def versionWithName(channelId: Int, versionString: String): Future[Option[Version]] = {
    get[VersionTable, Version](classOf[Version], v => v.channelId === channelId && v.versionString === versionString)
  }

  def versionWithId(id: Int): Future[Option[Version]] = get[VersionTable, Version](classOf[Version], v => v.id === id)

  def createVersion(version: Version): Future[Version] = {
    // copy new vals into old version
    version.onCreate()
    val versions = q[VersionTable](classOf[Version])
    val query = {
      versions returning versions.map(_.id) into {
        case (v, id) =>
          v.copy(id=Some(id))
      } += version
    }
    this.config.db.run(query)
  }

  def deleteVersion(version: Version) = {
    val query = _filter[VersionTable, Version](classOf[Version], v => v.id === version.id.get)
    val action = query.delete
    this.config.db.run(action)
  }

  def hasVersionBeenDownloadedBy(version: Version, cookie: String): Future[Boolean] = {
    val query = this.versionDownloads.filter(vd => vd.versionId === version.id.get && vd.cookie === cookie).size > 0
    this.config.db.run(query.result)
  }

  def setVersionDownloadedBy(version: Version, cookie: String) = {
    val query = this.versionDownloads += (None, Some(cookie), None, version.id.get)
    this.config.db.run(query)
  }

  def hasVersionBeenDownloadedBy(version: Version, user: User): Future[Boolean] = {
    val query = this.versionDownloads.filter(vd => vd.versionId === version.id.get && vd.userId === user.externalId).size > 0
    this.config.db.run(query.result)
  }

  def setVersionDownloadedBy(version: Version, user: User) = {
    val query = this.versionDownloads += (None, None, Some(user.externalId), version.id.get)
    this.config.db.run(query)
  }

  def updateVersionInt(version: Version, key: VersionTable => Rep[Int], value: Int): Future[Int] = {
    val versions = q[VersionTable](classOf[Version])
    val query = for { v <- versions if v.id === version.id.get } yield key(v)
    val action = query.update(value)
    this.config.db.run(action)
  }

  def updateVersionString(version: Version, key: VersionTable => Rep[String], value: String): Future[Int] = {
    val versions = q[VersionTable](classOf[Version])
    val query = for { v <- versions if v.id === version.id.get } yield key(v)
    val action = query.update(value)
    this.config.db.run(action)
  }

  // Page queries

  def pagesInProject(projectId: Int): Future[Seq[Page]] = {
    filter[PagesTable, Page](classOf[Page], p => p.projectId === projectId)
  }

  def pageWithName(projectId: Int, name: String): Future[Option[Page]] = {
    get[PagesTable, Page](classOf[Page], p => p.projectId === projectId
      && p.name.toLowerCase === name.toLowerCase)
  }

  def getOrCreatePage(page: Page): Page = {
    now(pageWithName(page.projectId, page.name)).get.getOrElse(now(createPage(page)).get)
  }

  def createPage(page: Page) = {
    page.onCreate()
    val pages = q[PagesTable](classOf[Page])
    val query = {
      pages returning pages.map(_.id) into {
        case (p, id) =>
          p.copy(id=Some(id))
      } += page
    }
    this.config.db.run(query)
  }

  def deletePage(page: Page) = {
    val query = _filter[PagesTable, Page](classOf[Page], p => p.id === page.id.get)
    this.config.db.run(query.delete)
  }

  def updatePageString(page: Page, key: PagesTable => Rep[String], value: String): Future[Int] = {
    val pages = q[PagesTable](classOf[Page])
    val query = for { p <- pages if p.id === page.id.get } yield key(p)
    this.config.db.run(query.update(value))
  }

}
