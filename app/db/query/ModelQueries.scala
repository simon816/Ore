package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import Queries._
import db.{Model, ModelTable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}

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
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def create(model: M): Future[M] = {
    val toInsert = copyInto(None, Some(theTime), model)
    val models = q[T](toInsert.getClass)
    val query = {
      models returning models.map(_.pk) into {
        case (m, id) =>
          copyInto(Some(id), m.createdAt, m)
      } += toInsert
    }
    DB.run(query)
  }

  /**
    * Returns the specified model or creates it if it doesn't exist.
    *
    * @param model  Model to get or create
    * @return       Existing or newly created model
    */
  def getOrCreate(model: M): Future[M] = {
    val modelPromise = Promise[M]
    named(model).onComplete {
      case Failure(thrown) => modelPromise.failure(thrown)
      case Success(modelOpt) => modelOpt match {
        case Some(existing) =>
          modelPromise.success(existing)
        case None =>
          modelPromise.completeWith(create(model))
      }
    }
    modelPromise.future
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

  /**
    * Creates a copy of the specified model with the specified ID.
    *
    * @param id       ID to put in model
    * @param theTime  Current time
    * @param model    Model to copy
    * @return         Copy of model
    */
  def copyInto(id: Option[Int], theTime: Option[Timestamp], model: M): M

  /**
    * Tries to find the specified model in it's table with an unset ID.
    *
    * @param model  Model to find
    * @return       Model if found
    */
  def named(model: M): Future[Option[M]] = Future(None)

}
