package db.key

import db.Model

/**
  * Maps a [[Model]]'s field to the corresponding [[db.ModelTable]] column.
  *
  * @tparam M Model type
  */
trait Key[M <: Model] {

  /**
    * Updates the model in the database.
    *
    * @param model Model to update
    */
  def update(model: M)

}
