package db.impl.model.common

import db.Model
import db.impl.table.common.EmailColumn

/**
  * Represents a [[Model]] with an email.
  */
trait Emailable extends Model { self =>

  override type M <: Emailable { type M = self.M }
  override type T <: EmailColumn[M]

  /**
    * The model's email.
    *
    * @return Model's email
    */
  def email: Option[String]

}
