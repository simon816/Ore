package db.table.key

import db.Model
import db.impl.OrePostgresDriver.api._
import slick.jdbc.JdbcType

case class TypeKey[M <: Model, A](ref: M#T => Rep[A], getter: M => A)(implicit mapper: JdbcType[A]) extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.set(model, this.ref, getter(model))
  }

}
