package db.table.key

import java.sql.Timestamp

import db.Model
import db.impl.OrePostgresDriver.api._

case class TimestampKey[M <: Model](ref: M#T => Rep[Timestamp], getter: M => Timestamp)
  extends Key[M] {

  def update(model: M) = {
    val service = model.service
    service.await(service.setTimestamp(model, this.ref, this.getter(model)))
  }

}
