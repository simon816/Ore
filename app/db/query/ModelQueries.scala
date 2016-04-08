package db.query

import db.OrePostgresDriver.api._
import Queries._
import db.{Model, ModelTable}

/**
  * Base class for handling Model queries.
  *
  * @tparam T ModelTable
  * @tparam M Model
  */
trait ModelQueries[T <: ModelTable[M], M <: Model] {

  /**
    * Sets an Int field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setInt(model: M, key: T => Rep[Int], value: Int) = {
    val models = q[T](model.getClass)
    val query = for { m <- models if m.pk === model.id.get } yield key(m)
    DB.run(query.update(value))
  }

  /**
    * Sets a String field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setString(model: M, key: T => Rep[String], value: String) = {
    val models = q[T](model.getClass)
    val query = for { m <- models if m.asInstanceOf[T].pk === model.id.get } yield key(m)
    DB.run(query.update(value))
  }

  /**
    * Deletes the specified Model.
    *
    * @param model Model to delete
    */
  def delete(model: M) = {
    val query = filterQuery[T, M](model.getClass, m => m.pk === model.id.get)
    DB.run(query.delete)
  }

}
