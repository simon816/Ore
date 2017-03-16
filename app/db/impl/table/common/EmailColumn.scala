package db.impl.table.common

import db.impl.OrePostgresDriver.api._
import db.impl.model.common.Emailable
import db.table.ModelTable

trait EmailColumn[M <: Emailable] extends ModelTable[M] {

  def email = column[String]("email")

}
