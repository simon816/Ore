package db.impl.access

import db.impl.FlagTable
import db.{ModelBase, ModelService}
import models.project.Flag

class FlagBase(override val service: ModelService) extends ModelBase[FlagTable, Flag] {

  override val modelClass: Class[Flag] = classOf[Flag]

  /**
    * Returns all Flags that are unresolved.
    *
    * @return All unresolved flags
    */
  def unresolved: Seq[Flag] = this.filterNot(_.isResolved)

}
