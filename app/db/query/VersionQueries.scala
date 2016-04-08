package db.query

import db.OrePostgresDriver.api._
import Queries._
import db.{VersionDownloadsTable, VersionTable}
import models.auth.User
import models.project.Version

import scala.concurrent.Future

/**
  * Version related queries.
  */
object VersionQueries extends ModelQueries[VersionTable, Version] {

  protected[db] val downloads = TableQuery[VersionDownloadsTable]

  /**
    * Returns all Versions in the specified Project.
    *
    * @param projectId  Project ID
    * @return           Versions in project
    */
  def inProject(projectId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.projectId === projectId)
  }

  /**
    * Returns all Versions in the specified Channel
    *
    * @param channelId  Channel ID
    * @return           Versions in channel
    */
  def inChannel(channelId: Int): Future[Seq[Version]] = {
    filter[VersionTable, Version](classOf[Version], v => v.channelId === channelId)
  }

  /**
    * Returns all Versions in the specified seq of channels.
    *
    * @param channelIds   Channel IDs
    * @return             Versions in the Channel ID seq
    */
  def inChannels(channelIds: Seq[Int]): Future[Seq[Version]] = {
    val query = for {
      version <- q[VersionTable](classOf[Version])
      if version.channelId inSetBind channelIds
    } yield version
    DB.run(query.result)
  }

  /**
    * Returns the Version with the specified name in the specified Channel.
    *
    * @param channelId      Channel version is in
    * @param versionString  Version to search for
    * @return               Version with name
    */
  def withName(channelId: Int, versionString: String): Future[Option[Version]] = {
    find[VersionTable, Version](classOf[Version], v => v.channelId === channelId && v.versionString === versionString)
  }

  /**
    * Returns the Version with the specified ID.
    *
    * @param id   Version ID
    * @return     Version with ID
    */
  def withId(id: Int): Future[Option[Version]] = find[VersionTable, Version](classOf[Version], v => v.id === id)

  /**
    * Creates a new Version.
    *
    * @param version  Version to create
    * @return         Newly created Version
    */
  def create(version: Version): Future[Version] = {
    // copy new vals into old version
    version.onCreate()
    val versions = q[VersionTable](classOf[Version])
    val query = {
      versions returning versions.map(_.id) into {
        case (v, id) =>
          v.copy(id=Some(id))
      } += version
    }
    DB.run(query)
  }

  /**
    * Returns true if the specified Version has been downloaded by a client
    * with the specified cookie.
    *
    * @param versionId  Version to check
    * @param cookie     Cookie to look for
    * @return           True if downloaded
    */
  def hasBeenDownloadedBy(versionId: Int, cookie: String): Future[Boolean] = {
    val query = downloads.filter(vd => vd.versionId === versionId && vd.cookie === cookie).size > 0
    DB.run(query.result)
  }

  /**
    * Sets the specified Version as downloaded by the client with the specified
    * cookie.
    *
    * @param versionId  Version to set as downloaded
    * @param cookie     To set as downloaded for
    */
  def setDownloadedBy(versionId: Int, cookie: String) = {
    val query = downloads += (None, Some(cookie), None, versionId)
    DB.run(query)
  }

  /**
    * Returns true if the specified Version has been downloaded by the
    * specified User.
    *
    * @param versionId  Version to check
    * @param userId     User to look for
    * @return           True if downloaded
    */
  def hasBeenDownloadedBy(versionId: Int, userId: Int): Future[Boolean] = {
    val query = downloads.filter(vd => vd.versionId === versionId && vd.userId === userId).size > 0
    DB.run(query.result)
  }

  /**
    * Sets the specified Version as downloaded by the specified User.
    *
    * @param versionId  Version to set as downloaded
    * @param userId     To set as downloaded for
    */
  def setDownloadedBy(versionId: Int, userId: Int) = {
    val query = downloads += (None, None, Some(userId), versionId)
    DB.run(query)
  }

}
