package db.model

import db.driver.OrePostgresDriver.api._

/**
  * Represents a one-to-many relationship.
  *
  * @param childClass   Child model class
  * @param ref          Reference column to parent in child table
  */
case class ManyBinding(childClass: Class[_ <: Model], ref: ModelTable[_] => Rep[Int])
