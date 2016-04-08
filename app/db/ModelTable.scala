package db

import db.OrePostgresDriver.api._
import slick.lifted.Tag

/**
  * Represents a Table in the database that contains Models.
  *
  * @param tag    Table tag
  * @param name   Table name
  * @tparam M     The model this Table contains
  */
abstract class ModelTable[M <: Model](tag: Tag, name: String) extends Table[M](tag, name) {

  /**
    * Returns this table's primary key.
    *
    * @return Table primary key
    */
  def pk: Rep[Int]

}
