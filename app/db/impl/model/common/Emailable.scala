package db.impl.model.common

import db.Model
import db.impl.table.common.EmailColumn

trait Emailable extends Model { self =>

  override type M <: Emailable { type M = self.M }
  override type T <: EmailColumn[M]

  def email: Option[String]

}
