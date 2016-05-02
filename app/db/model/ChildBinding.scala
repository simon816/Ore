package db.model

import db.driver.OrePostgresDriver.api._

/**
  * Represents a one-to-many relationship.
  *
  * @param childClass   Child model class
  * @param ref          Reference column to parent in child table
  * @tparam ChildTable  Child table
  * @tparam Child       Child model
  */
case class ChildBinding[ChildTable <: ModelTable[Child], Child <: Model](childClass: Class[_ <: Child],
                                                                         ref: ChildTable => Rep[Int])
