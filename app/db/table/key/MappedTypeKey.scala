package db.table.key

import db.impl.OrePostgresDriver.api._
import db.Model
import db.table.MappedType

case class MappedTypeKey[M <: Model, A <: MappedType[A]](ref: M#T => Rep[A], getter: M => A) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setMappedType(model, this.ref, this.getter(model)))
  }

}
