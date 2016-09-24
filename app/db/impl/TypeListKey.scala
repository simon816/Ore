package db.impl

import db.impl.OrePostgresDriver.api._
import db.key.Key
import db.{Model, MappedType}
import slick.jdbc.JdbcType

case class TypeListKey[M <: Model, A <: MappedType[_]](ref: M#T => Rep[List[A]],
                                                       getter: M => List[A],
                                                       mapper: JdbcType[List[A]]) extends Key[M] {
  def update(model: M) = {
//    val service = model.service
//    service.await(service.setType[MappedType[List[A]], M](model, this.ref, new MappedType[List[A]] {
//      implicit val mapper: JdbcType[Seq[A]] = this.mapper
//    }))
  }

}
