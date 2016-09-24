package db.key

import db.Model
import db.impl.OrePostgresDriver.api._

case class StringKey[M <: Model](ref: M#T => Rep[String], getter: M => String) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setString(model, this.ref, this.getter(model)))
  }

}
