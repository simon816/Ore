package db.meta

import db.impl.OrePostgresDriver.api._
import db.{Model, ModelTable}

/**
  * Represents a one-to-many relationship.
  *
  * @param childClass   Child model class
  * @param ref          Reference column to parent in child table
  */
case class OneToManyBinding(childClass: Class[_ <: Model], ref: ModelTable[_] => Rep[Int])
