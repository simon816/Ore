package db.impl.query

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.{VersionDownloadsTable, VersionTable}
import db.query.{ModelFilter, ModelQueries, StatQueries}
import models.project.Version
import models.statistic.VersionDownload

import scala.concurrent.Future

/**
  * Version related queries.
  */
class VersionQueries(implicit val service: ModelService) extends ModelQueries[VersionTable, Version](
  classOf[Version], TableQuery[VersionTable]) {

  val Downloads = service.registrar.register(new StatQueries[VersionDownloadsTable, VersionDownload](
    classOf[VersionDownload], TableQuery[VersionDownloadsTable]
  ))

  /**
    * Returns true if the specified hash is found in the specified
    * [[models.project.Project]]'s [[Version]]s.
    *
    * @param projectId  Project ID
    * @param hash       Version hash
    * @return           True if found
    */
  def hashExists(projectId: Int, hash: String): Future[Boolean] = service.run(((for {
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

}
