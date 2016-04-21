package db.orm

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.orm.model.Model
import slick.lifted.Tag

/**
  * Represents a Table in the database that contains Models.
  *
  * @param tag    Table tag
  * @param name   Table name
  * @tparam M     The model this Table contains
  */
abstract class ModelTable[M <: Model](tag: Tag, name: String) extends Table[M](tag, name) {

  def id          =   column[Int]("id", O.PrimaryKey, O.AutoInc)
  def createdAt   =   column[Timestamp]("created_at")

}
