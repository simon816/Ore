package db.impl.access

import db.impl.VersionTable
import db.{ModelBase, ModelService}
import models.project.Version

class VersionBase(override val service: ModelService) extends ModelBase[VersionTable, Version] {

  override val modelClass = classOf[Version]

  /**
    * Returns all Versions that have not been reviewed by the moderation staff.
    *
    * @return All versions not reviewed
    */
  def notReviewed: Seq[Version] = this.filterNot(_.isReviewed)

}
