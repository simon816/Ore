package db.key

import db.impl.OrePostgresDriver.api._
import db.{Model, MappedType}

case class TypeKey[M <: Model, A <: MappedType[A]](ref: M#T => Rep[A], getter: M => A) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setType(model, this.ref, this.getter(model)))
  }

}
