package db.impl.table.common

import db.impl.OrePostgresDriver.api._
import db.impl.model.common.Hideable
import db.table.ModelTable

/**
  * Represents a column in a [[ModelTable]] representing the visibility of the
  * model.
  *
  * @tparam M Model type
  */
trait VisibilityColumn[M <: Hideable] extends ModelTable[M] {

  /**
    * Column definition of visibility. True if visible.
    *
    * @return Visibility column
    */
  def isVisible = column[Boolean]("is_visible")

}
