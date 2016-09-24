package db.key

import db.Model
import db.impl.OrePostgresDriver.api._

case class BooleanKey[M <: Model](ref: M#T => Rep[Boolean], getter: M => Boolean) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setBoolean(model, this.ref, this.getter(model)))
  }

}
