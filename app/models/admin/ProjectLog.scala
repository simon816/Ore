package models.admin

import java.sql.Timestamp

import db.ModelFilter
import db.access.ModelAccess
import db.impl.OrePostgresDriver.api._
import db.impl.ProjectLogTable
import db.impl.model.OreModel
import ore.project.ProjectOwned

case class ProjectLog(override val id: Option[Int] = None,
                      override val createdAt: Option[Timestamp] = None,
                      override val projectId: Int)
                      extends OreModel(id, createdAt) with ProjectOwned {

  override type T = ProjectLogTable
  override type M = ProjectLog

  def entries: ModelAccess[ProjectLogEntry] = this.schema.getChildren[ProjectLogEntry](classOf[ProjectLogEntry], this)

  def err(message: String): ProjectLogEntry = Defined {
    val entries = this.service.access[ProjectLogEntry](
      classOf[ProjectLogEntry], ModelFilter[ProjectLogEntry](_.logId === this.id.get))
    val tag = "error"
    entries.find(e => e.message === message && e.tag === tag).map { entry =>
      entry.occurrences = entry.occurrences + 1
      entry.lastOccurrence = this.service.theTime
      entry
    } getOrElse {
      entries.add(ProjectLogEntry(
        logId = this.id.get,
        tag = tag,
        message = message,
        _lastOccurrence = this.service.theTime))
    }
  }

  def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
