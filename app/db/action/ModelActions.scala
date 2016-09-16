package db.action

import db.impl.pg.OrePostgresDriver.api._
import db.meta.ModelLink
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
class ModelActions[ThisTable <: ModelTable[ThisModel], ThisModel <: Model: TypeTag](val service: ModelService,
                                                                    val modelClass: Class[ThisModel],
                                                                    val baseQuery: TableQuery[ThisTable]) {

  /** Model filter alias */
  type Filter = ThisTable => Rep[Boolean]
  type Ordering = ThisTable => ColumnOrdered[_]

  private var links: Map[Class[_ <: Table[_]], ModelLink] = Map.empty

  // Model specific

  /**
    * Adds a new [[ModelLink]] to this ModelActions instance, to use the
    * [[db.meta.relation.ManyToManyCollection]] binding, one must register
    * their mediator class here.
    *
    * @param targetClass    Model to target
    * @param linkTableClass Mediator table class
    * @param linkTable      Mediator table query
    * @tparam Link          Table type
    * @return               This instance
    */
  def withLink[Link <: Table[_]](targetClass: Class[_ <: Model],
                                 linkTableClass: Class[Link],
                                 linkTable: TableQuery[Link]) = {
    this.links += linkTableClass -> ModelLink(targetClass, linkTable)
    this
  }

  /**
    * Returns a [[ModelLink]] for the specified mediator table class. Used for
    * many-to-many relationships.
    *
    * @param linkTableClass Mediator table
    * @return ModelLink for table
    */
  def getLink(linkTableClass: Class[_ <: Table[_]]) = {
    this.links.getOrElse(linkTableClass, throw new RuntimeException("No link found for table " + linkTableClass
      + ". Did you add it to your ModelActions? (see: withLink())"))
  }

  /**
    * Returns the specified model or creates it if it doesn't exist.
    *
    * @param model  Model to get or create
    * @return       Existing or newly created model
    */
  def getOrInsert(model: ThisModel): Future[ThisModel] = {
    val modelPromise = Promise[ThisModel]
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
  def like(model: ThisModel): Future[Option[ThisModel]] = Future(None)

  // Generic (delegate to service)

  def insert(model: ThisModel) = this.service.insert(model)
  def find(filter: Filter) = this.service.find(modelClass, filter)
  def count(filter: Filter = null) = this.service.count(modelClass, filter)
  def delete(model: ThisModel) = this.service.delete(model)
  def deleteWhere(filter: Filter) = this.service.deleteWhere(modelClass, filter)
  def get(id: Int, filter: ThisTable => Rep[Boolean]) = this.service.get(modelClass, id, filter)
  def collect(filter: Filter = null, sort: Ordering = null, limit: Int = -1, offset: Int = -1)
  = this.service.collect(modelClass, filter, sort, limit, offset)
  def filter(filter: Filter, limit: Int = -1, offset: Int = -1)
  = this.service.filter(modelClass, filter, limit, offset)
  def sorted(sort: Ordering, filter: Filter = null, limit: Int = -1, offset: Int = -1)
  = this.service.sorted(modelClass, sort, filter, limit, offset)

}
