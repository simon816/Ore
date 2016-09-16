package db

import db.impl.pg.OrePostgresDriver.api._

/**
  * Represents a associative table between two models.
  *
  * @param tag Table tag
  * @param name Table name
  */
abstract class AssociativeTable(tag: Tag, name: String) extends Table[(Int, Int)](tag, name)
