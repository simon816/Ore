package db.meta

import db.{Model, ModelTable}
import db.impl.pg.OrePostgresDriver.api._

/**
  * Represents a many-to-many relationship between two models. This is handled
  * by a mediator table that tracks relationships between the two models.
  *
  * @param childClass       Target model class
  * @param table            Target model TableQuery instance
  * @param selfRef          Reference to this model in mediator table
  * @param otherRef         Reference to target model in mediator table
  * @param finalRef         Reference to target model in it's ModelTable
  * @tparam RelationsTable  Mediator table type
  */
case class ManyToManyBinding[RelationsTable <: Table[_]](childClass: Class[_ <: Model],
                                                         table: TableQuery[RelationsTable],
                                                         selfRef: RelationsTable => Rep[Int],
                                                         otherRef: RelationsTable => Rep[Int],
                                                         finalRef: ModelTable[_ <: Model] => Rep[Int])
