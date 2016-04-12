package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.query.Queries.DB._
import db.{VersionDownloadsTable, VersionTable}
import models.project.Version

import scala.concurrent.Future

/**
  * Version related queries.
  */
class VersionQueries extends Queries[VersionTable, Version](TableQuery(tag => new VersionTable(tag))) {

  private val downloads = TableQuery[VersionDownloadsTable]

  /**
    * Returns all Versions in the specified seq of channels.
    *
    * @param channelIds   Channel IDs
    * @return             Versions in the Channel ID seq
    */
  def inChannels(channelIds: Seq[Int]): Future[Seq[Version]] = {
    val query = for {
      version <- this.models
      if version.channelId inSetBind channelIds
    } yield version
    run(query.result)
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
    val query = this.downloads.filter(vd => vd.versionId === versionId && vd.cookie === cookie).size > 0
    run(query.result)
  }

  /**
    * Sets the specified Version as downloaded by the client with the specified
    * cookie.
    *
    * @param versionId  Version to set as downloaded
    * @param cookie     To set as downloaded for
    */
  def setDownloadedBy(versionId: Int, cookie: String) = {
    val query = this.downloads += (None, Some(cookie), None, versionId)
    run(query)
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
    val query = this.downloads.filter(vd => vd.versionId === versionId && vd.userId === userId).size > 0
    run(query.result)
  }

  /**
    * Sets the specified Version as downloaded by the specified User.
    *
    * @param versionId  Version to set as downloaded
    * @param userId     To set as downloaded for
    */
  def setDownloadedBy(versionId: Int, userId: Int) = {
    val query = this.downloads += (None, None, Some(userId), versionId)
    run(query)
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], version: Version): Version = {
    version.copy(id = id, createdAt = theTime)
  }

}
