package db.query

import db.{ModelService, StatTable}
import db.impl.OrePostgresDriver.api._
import models.statistic.StatEntry

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

class StatQueries[T <: StatTable[M], M <: StatEntry[_, _]](statEntryClass: Class[M],
                                                           baseQuery: TableQuery[T])
                                                          (implicit service: ModelService)
  extends ModelQueries(statEntryClass, baseQuery) {

  /**
    * Checks if the specified StatEntry exists and records the entry in the
    * database by either inserting a new entry or updating an existing entry
    * with the User ID if applicable. Returns true if recorded in database.
    *
    * @param entry  Entry to check
    * @return       True if recorded in database
    */
  def record(entry: M): Future[Boolean] = {
    val promise = Promise[Boolean]
    this.like(entry).andThen {
      case result => result.get match {
        case None =>
          // No previous entry found, insert new entry
          promise.completeWith(insert(entry).map(_.isDefined))
        case Some(existingEntry) =>
          // Existing entry found, update the User ID if possible
          if (existingEntry.user.isEmpty && entry.user.isDefined) {
            existingEntry.user = entry.user.get
          }
          promise.success(false)
      }
    }
    promise.future
  }

  override def like(entry: M): Future[Option[M]] = {
    val baseFilter: ModelFilter[T, M] = ModelFilter[T, M](_.modelId === entry.modelId)
    val filter: Filter = e => e.address === entry.address || e.cookie === entry.cookie
    val userFilter = entry.user.map[Filter](u => e => filter(e) || e.userId === u.id.get).getOrElse(filter)
    this.find(baseFilter &&^ userFilter)
  }

}
