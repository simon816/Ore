package db.table.key

import db.Model
import db.table.ModelTable

/**
  * Maps a [[Model]]'s field to the corresponding [[ModelTable]] column.
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
