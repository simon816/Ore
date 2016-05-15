package db.impl.action

import db.ModelService
import db.action.{ModelActions, ModelFilter, StatActions}
import db.impl.OrePostgresDriver.api._
import db.impl.{VersionDownloadsTable, VersionTable}
import models.project.Version
import models.statistic.VersionDownload

import scala.concurrent.Future

/**
  * Version related queries.
  */
class VersionActions(override val service: ModelService)
  extends ModelActions[VersionTable, Version](service, classOf[Version], TableQuery[VersionTable]) {

  /** The [[StatActions]] for [[VersionDownload]]s. */
  val DownloadActions = service.registrar.register(
    new StatActions[VersionDownloadsTable, VersionDownload](
      this.service, TableQuery[VersionDownloadsTable], classOf[VersionDownload]
    )
  )

  /**
    * Returns true if the specified hash is found in the specified
    * [[models.project.Project]]'s [[Version]]s.
    *
    * @param projectId  Project ID
    * @param hash       Version hash
    * @return           True if found
    */
  def hashExists(projectId: Int, hash: String): Future[Boolean] = service.DB.db.run(((for {
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
