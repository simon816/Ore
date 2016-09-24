package db.impl.model

import db.Model
import db.impl.table.DescriptionColumn

/**
  * Represents a [[Model]] with a description.
  */
trait Describable extends Model { self =>

  override type M <: Describable { type M = self.M }
  override type T <: DescriptionColumn[M]

  /**
    * Returns the [[Model]]'s description.
    *
    * @return Model description
    */
  def description: Option[String]

}
