package db

import db.table.NameColumn

/**
  * Represents a [[Model]] with a name.
  */
trait Named extends Model { self =>

  override type M <: Named { type M = self.M }
  override type T <: NameColumn[M]

  /**
    * The model's name.
    *
    * @return Model name
    */
  def name: String

}
