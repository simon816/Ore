package db.table.key

import db.Model
import db.impl.OrePostgresDriver.api._

case class IntKey[M <: Model](ref: M#T => Rep[Int], getter: M => Int) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setInt(model, this.ref, this.getter(model)))
  }

}
