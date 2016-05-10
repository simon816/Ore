package db.action

import db.impl.OrePostgresDriver.api._
import db.{Model, ModelService, ModelTable}
import slick.lifted.ColumnOrdered

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

/**
  * Base class for handling Model queries. ModelActions define how Models can
  * interact with the database.
  */
class ModelActions[Table <: ModelTable[Row], Row <: Model: TypeTag](val modelClass: Class[Row],
                                                              val baseQuery: TableQuery[Table])
                                                             (implicit service: ModelService) {

  /** Model filter alias */
  type Filter = Table => Rep[Boolean]

  // Generic (delegate to service)

  def insert(model: Row) = service.insert(model)
  def find(predicate: Table => Rep[Boolean]) = service.find(modelClass, predicate)
  def count(filter: Table => Rep[Boolean] = null) = service.count(modelClass, filter)
  def delete(model: Row) = service.delete(model)
  def deleteWhere(filter: Table => Rep[Boolean]) = service.deleteWhere(modelClass, filter)
  def get(id: Int, filter: Table => Rep[Boolean]) = service.get(modelClass, id, filter)
  def collect(limit: Int = -1, offset: Int = -1, filter: Table => Rep[Boolean] = null,
              sort: Table => ColumnOrdered[_] = null)
  = service.collect(modelClass, filter, sort, limit, offset)
  def filter(filter: Table => Rep[Boolean], limit: Int = -1, offset: Int = -1)
  = service.filter(modelClass, filter, limit, offset)

  // Model specific

  /**
    * Returns the specified model or creates it if it doesn't exist.
    *
    * @param model  Model to get or create
    * @return       Existing or newly created model
    */
  def getOrInsert(model: Row): Future[Row] = {
    val modelPromise = Promise[Row]
    like(model).onComplete {
      case Failure(thrown) => modelPromise.failure(thrown)
      case Success(modelOpt) => modelOpt match {
        case Some(existing) => modelPromise.success(existing)
        case None => modelPromise.completeWith(service insert model)
      }
    }
    modelPromise.future
  }

  /**
    * Tries to find the specified model in it's table with an unset ID.
    *
    * @param model  Model to find
    * @return       Model if found
    */
  def like(model: Row): Future[Option[Row]] = Future(None)

}
