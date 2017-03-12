package models.admin

import java.sql.Timestamp

import db.impl.ProjectLogEntryTable
import db.impl.model.OreModel
import db.impl.table.ModelKeys._

case class ProjectLogEntry(override val id: Option[Int] = None,
                           override val createdAt: Option[Timestamp] = None,
                           logId: Int,
                           tag: String,
                           message: String,
                           private var _occurrences: Int = 1,
                           private var _lastOccurrence: Timestamp)
                           extends OreModel(id, createdAt) {

  override type T = ProjectLogEntryTable
  override type M = ProjectLogEntry

  def occurrences: Int = this._occurrences

  def occurrences_=(occurrences: Int) = Defined {
    this._occurrences = occurrences
    update(Occurrences)
  }

  def lastOccurrence: Timestamp = this._lastOccurrence

  def lastOccurrence_=(lastOccurrence: Timestamp) = Defined {
    this._lastOccurrence = lastOccurrence
    update(LastOccurrence)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
