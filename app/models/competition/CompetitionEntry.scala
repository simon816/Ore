package models.competition

import java.sql.Timestamp

import db.impl.CompetitionEntryTable
import db.impl.model.OreModel
import ore.project.ProjectOwned
import ore.user.UserOwned

/**
  * Represents a single entry in a [[Competition]].
  *
  * @param id             Unique ID
  * @param createdAt      Instant of creation
  * @param projectId      Project ID
  * @param userId         User owner ID
  * @param competitionId  Competition ID
  */
case class CompetitionEntry(override val id: Option[Int] = None,
                            override val createdAt: Option[Timestamp] = None,
                            override val projectId: Int,
                            override val userId: Int,
                            competitionId: Int)
                            extends OreModel(id, createdAt) with ProjectOwned with UserOwned {

  override type T = CompetitionEntryTable
  override type M = CompetitionEntry

  /**
    * Returns the [[Competition]] this entry belongs to.
    *
    * @return Competition entry belongs to
    */
  def competition: Competition = this.service.access[Competition](classOf[Competition]).get(this.competitionId).get

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
