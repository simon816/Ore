package db.impl.access

import db.{ModelBase, ModelService}
import models.project.Flag

/**
  * FIXME: This class is unnecessary
  */
class FlagBase(override val service: ModelService) extends ModelBase[Flag] {

  override val modelClass: Class[Flag] = classOf[Flag]

  /**
    * Returns all Flags that are unresolved.
    *
    * @return All unresolved flags
    */
  def unresolved: Seq[Flag] = this.filterNot(_.isResolved)

}
