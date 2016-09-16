package db.meta

import db.impl.pg.OrePostgresDriver.api._
import db.Model

/**
  * Represents a model that is linked to another through a mediator table.
  *
  * @param targetClass  model class
  * @param linkTable    mediator table
  */
case class ModelLink(targetClass: Class[_ <: Model], linkTable: TableQuery[_ <: Table[_]])
