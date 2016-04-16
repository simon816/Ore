package db.orm.dao

import db.orm.model.Model

/**
  * A basic Model data access object.
  */
trait ModelDAO[M <: Model] {

  /**
    * Returns the model with the specified ID.
    *
    * @param id   ID to lookup
    * @return     Model with ID or None if not found
    */
  def withId(id: Int): Option[M]

}
