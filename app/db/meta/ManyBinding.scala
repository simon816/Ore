package db.meta

import db.{Model, ModelTable}
import db.impl.OrePostgresDriver.api._

/**
  * Represents a one-to-many relationship.
  *
  * @param childClass   Child model class
  * @param ref          Reference column to parent in child table
  */
case class ManyBinding(childClass: Class[_ <: Model[_]], ref: ModelTable[_] => Rep[Int])
