package db.impl.table.common

import db.impl.OrePostgresDriver.api._
import db.impl.model.common.Emailable
import db.table.ModelTable

/**
  * Model table column for a model email.
  *
  * @tparam M Emailable model
  */
trait EmailColumn[M <: Emailable] extends ModelTable[M] {

  /**
    * The email column
    *
    * @return Email column
    */
  def email = column[String]("email")

}
