package db.impl.table

import db.impl.OrePostgresDriver.api._
import db.impl.model.Describable
import db.table.ModelTable

/**
  * Represents a column in a [[ModelTable]] that holds a description.
  *
  * @tparam M Model type
  */
trait DescriptionColumn[M <: Describable] extends ModelTable[M] {

  /**
    * The [[ModelTable]]'s description column.
    *
    * @return Description column
    */
  def description = column[String]("description")

}
