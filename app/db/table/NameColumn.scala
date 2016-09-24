package db.table

import db.impl.OrePostgresDriver.api._
import db.{Model, Named}

/**
  * A table column to represent a [[Model]]'s name.
  *
  * @tparam M Model type
  */
trait NameColumn[M <: Named] extends ModelTable[M] {

  /**
    * The model's name column.
    *
    * @return Model name column
    */
  def name = column[String]("name")

}
