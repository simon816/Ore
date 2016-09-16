package db.meta.relation

import db.impl.pg.OrePostgresDriver.api._
import db.Model

/**
  * Represents a many-to-many relationship between two models. This is handled
  * by a mediator table that tracks relationships between the two models.
  *
  * @param childClass       Target model class
  * @param table            Target model TableQuery instance
  * @param selfRef          Reference to this model in mediator table
  * @param otherRef         Reference to target model in mediator table
  */
case class ManyToManyBinding(childClass: Class[_ <: Model],
                             table: TableQuery[_ <: Table[_]],
                             selfRef: Table[_] => Rep[Int],
                             otherRef: Table[_] => Rep[Int])
