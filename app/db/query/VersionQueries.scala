package db.query

import db.OrePostgresDriver.api._
import db.query.Queries.{ModelFilter, run}
import db.{VersionDownloadsTable, VersionTable}
import models.project.Version

import scala.concurrent.Future

/**
  * Version related queries.
  */
class VersionQueries extends Queries {

  override type Row = Version
  override type Table = VersionTable

  private val downloads = TableQuery[VersionDownloadsTable]

  override val modelClass = classOf[Version]
  override val baseQuery = TableQuery[VersionTable]

  registerModel()

  /**
    * Returns true if the specified hash is found in the specified
    * [[models.project.Project]]'s [[Version]]s.
    *
    * @param projectId  Project ID
    * @param hash       Version hash
    * @return           True if found
    */
  def hashExists(projectId: Int, hash: String): Future[Boolean] = run(((for {
      model <- this.baseQuery
      if model.projectId === projectId
      if model.hash === hash
  } yield model.id).length > 0).result)

  /**
    * Returns a filter based on a Version's channel.
    *
    * @param channelIds Channel IDs to filter versions with
    * @return           Channel filter
    */
  def channelFilter(channelIds: Seq[Int]): ModelFilter[VersionTable, Version]
  = ModelFilter(_.channelId inSetBind channelIds)

  /**
    * Returns true if the specified Version has been downloaded by a client
    * with the specified cookie.
    *
    * @param versionId  Version to check
    * @param cookie     Cookie to look for
    * @return           True if downloaded
    */
  def hasBeenDownloadedBy(versionId: Int, cookie: String): Future[Boolean] = {
    val query = this.downloads.filter { vd =>
      vd.versionId === versionId && vd.cookie === cookie
    }.length > 0
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
  def hasBeenDownloadedBy(versionId: Int, userId: Int): Future[Boolean] = run((this.downloads.filter { vd =>
    vd.versionId === versionId && vd.userId === userId
  }.length > 0).result)

  /**
    * Sets the specified Version as downloaded by the specified User.
    *
    * @param versionId  Version to set as downloaded
    * @param userId     To set as downloaded for
    */
  def setDownloadedBy(versionId: Int, userId: Int) = run(this.downloads += (None, None, Some(userId), versionId))

}
