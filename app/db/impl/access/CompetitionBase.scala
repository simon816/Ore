package db.impl.access

import db.access.{ImmutableModelAccess, ModelAccess}
import db.impl.OrePostgresDriver.api._
import db.{ModelBase, ModelFilter, ModelService}
import models.competition.Competition

/**
  * Handles competition based database actions.
  *
  * @param service ModelService instance
  */
class CompetitionBase(override val service: ModelService) extends ModelBase[Competition] {

  override val modelClass = classOf[Competition]

  /**
    * Returns [[ModelAccess]] to all active competitions.
    *
    * @return Access to active competitions
    */
  def active: ModelAccess[Competition] = {
    val now = this.service.theTime
    ImmutableModelAccess(this.service, this.modelClass, ModelFilter[Competition] { competition =>
      competition.startDate <= now && competition.endDate > now
    })
  }

}
