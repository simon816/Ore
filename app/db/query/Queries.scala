package db.query

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.dao.ModelSet
import db.orm.model.Model
import db.query.user.UserQueries
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.{DBIOAction, NoStream}
import slick.driver.JdbcProfile
import slick.lifted.ColumnOrdered
import util.C
import util.C._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Base class for handling Model queries.
  */
abstract class Queries {

  /** The model to be retrieved from the Table */
  type Row <: Model
  /** The table to retrieve the model from */
  type Table <: ModelTable[Row]

  val modelClass: Class[Row]
  val baseQuery: TableQuery[Table]

  def registerModel() = Queries.registerModel(this.modelClass, this.baseQuery)

  // Generic (delegate to companion object)

  def find(predicate: Table => Rep[Boolean]): Future[Option[Row]] = Queries.find(modelClass, predicate)
  def count(filter: Table => Rep[Boolean] = null): Future[Int] = Queries.count(modelClass, filter)
  def insert(model: Row): Future[Row] = Queries.insert(model)
  def delete(model: Row): Future[Int] = Queries.delete(model)
  def deleteWhere(filter: Table => Rep[Boolean]) = Queries.deleteWhere(modelClass, filter)
  def get(id: Int): Future[Option[Row]] = Queries.get(modelClass, id)
  def setInt(model: Row, key: Table => Rep[Int], value: Int) = Queries.setInt(model, key, value)
  def setString(model: Row, key: Table => Rep[String], value: String) = Queries.setString(model, key, value)
  def setBoolean(model: Row, key: Table => Rep[Boolean], value: Boolean) = Queries.setBoolean(model, key, value)
  def setIntList(model: Row, key: Table => Rep[List[Int]], value: List[Int]) = Queries.setIntList(model, key, value)
  def setTimestamp(model: Row, key: Table => Rep[Timestamp], value: Timestamp) = Queries.setTimestamp(model, key, value)
  def collect(limit: Int = -1, offset: Int = -1, filter: Table => Rep[Boolean] = null,
              sort: Table => ColumnOrdered[_] = null): Future[Seq[Row]]
  = Queries.collect(modelClass, limit, offset, filter, sort)
  def filter(filter: Table => Rep[Boolean], limit: Int = -1, offset: Int = -1)
  = Queries.filter(modelClass, filter, limit, offset)

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
        case None => modelPromise.completeWith(insert(model))
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

/**
  * Contains all queries for retrieving models from the database.
  */
object Queries {

  // Setup registrar

  var modelQueries: Map[Class[_ <: Model], TableQuery[_ <: ModelTable[_]]] = Map()

  /**
    * Registers a new model and maps it to a base query.
    *
    * @param query Base query
    * @tparam M Model
    */
  protected def registerModel[T <: ModelTable[M], M <: Model](modelClass: Class[M], query: TableQuery[T]) = {
    this.modelQueries += modelClass -> query
    debug("-- Model registered --")
    debug("Registered: " + modelQueries)
    debug("Class: " + modelClass)
    debug("Query: " + query)
    debug("-----------------------")
  }

  /**
    * Returns a base query for a Model.
    *
    * @tparam T Model table
    * @tparam M Model
    * @return   Base model query
    */
  def modelQuery[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): Query[T, M, Seq] = {
    this.modelQueries.get(modelClass).get.asInstanceOf[Query[T, M, Seq]]
  }

  // Initialize queries (registers models)

  val Users     =   new UserQueries
  val Projects  =   new ProjectQueries
  val Channels  =   new ChannelQueries
  val Versions  =   new VersionQueries
  val Pages     =   new PageQueries

  protected[db] val Config = DatabaseConfigProvider.get[JdbcProfile](Play.current)
  protected val DB = Config.db
  protected[db] def theTime: Timestamp = new Timestamp(new Date().getTime)

  // Utilities

  /**
    * Runs the specified action on the DB.
    *
    * @param a  Action to run
    * @return   Result
    */
  def run[R](a: DBIOAction[R, NoStream, Nothing]): Future[R] = DB.run(a)

  /**
    * The default timeout when awaiting a query result.
    */
  val DefaultTimeout: Duration = Duration(AppConf.getInt("db.default-timeout").get, TimeUnit.SECONDS)

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f        Future to await
    * @param timeout  Timeout duration
    * @tparam M       Return type
    * @return         Try of return type
    */
  def now[M](f: Future[M], timeout: Duration = DefaultTimeout): Try[M] = Await.ready(f, timeout).value.get

  /**
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def insert[T <: ModelTable[M], M <: Model](model: M): Future[M] = {
    val toInsert = model.copyWith(None, Some(theTime)).asInstanceOf[M]
    val models = modelQuery[T, M](model.getClass)
    run {
      models returning models.map(_.id) into {
        case (m, id) =>
          model.copyWith(Some(id), m.createdAt).asInstanceOf[M]
      } += toInsert
    }
  }

  /**
    * Returns the first model that matches the given predicate.
    *
    * @param predicate  Predicate
    * @return           Optional result
    */
  def find[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M],
                                           predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val modelPromise = Promise[Option[M]]
    val query = modelQuery[T, M](modelClass).filter(predicate).take(1)
    run(query.result).andThen {
      case Failure(thrown) => modelPromise.failure(thrown)
      case Success(result) => modelPromise.success(result.headOption)
    }
    modelPromise.future
  }

  /**
    * Returns the size of the model table.
    *
    * @return Size of model table
    */
  def count[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M],
                                            filter: T => Rep[Boolean] = null): Future[Int] = {
    var query = modelQuery[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    run(query.length.result)
  }

  /**
    * Deletes the specified Model.
    *
    * @param model Model to delete
    */
  def delete[T <: ModelTable[M], M <: Model](model: M): Future[Int]
  = run(modelQuery[T, M](model.getClass).filter(_.id === model.id.get).delete)

  /**
    * Deletes all the models that match the specified filter.
    *
    * @param filter Filter to delete models
    */
  def deleteWhere[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M], filter: T => Rep[Boolean])
  = run(modelQuery[T, M](modelClass).filter(filter).delete)

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M], id: Int): Future[Option[M]]
  = find[T, M](modelClass, _.id === id)

  /**
    * Sets an Int field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setInt[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Int], value: Int)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a String field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setString[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[String], value: String)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a Boolean field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setBoolean[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Boolean], value: Boolean)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets an int array field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value
    */
  def setIntList[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[List[Int]], value: List[Int])
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Sets a Timestamp field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value
    */
  def setTimestamp[T <: ModelTable[M], M <: Model](model: M, key: T => Rep[Timestamp], value: Timestamp)
  = run((for { m <- modelQuery[T, M](model.getClass) if m.id === model.id.get } yield key(m)).update(value))

  /**
    * Returns a collection of models with the specified limit and offset.
    *
    * @param limit  Amount of models to take
    * @param offset Offset to drop
    * @return       Collection of models
    */
  def collect[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M], limit: Int = -1, offset: Int = -1,
                                              filter: T => Rep[Boolean] = null,
                                              sort: T => ColumnOrdered[_] = null): Future[Seq[M]] = {
    var query = modelQuery[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    if (sort != null) query = query.sortBy(sort)
    if (offset > -1) query = query.drop(offset)
    if (limit > -1) query = query.take(limit)
    run(query.result)
  }

  /**
    * Filters the the models.
    *
    * @param filter Model filter
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @tparam T     Model table
    * @tparam M     Model
    * @return       Filtered models
    */
  def filter[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
                                             limit: Int = -1, offset: Int = -1): Future[Seq[M]]
  = collect(modelClass, filter = filter, limit = limit, offset = offset)

  /**
    * Returns a [[ModelSet]] for the specified relation.
    *
    * @param parentRef    Reference to parent in ChildTable
    * @param parent       Parent model
    * @tparam ParentTable Parent table
    * @tparam Parent      Parent model
    * @tparam ChildTable  Child table
    * @tparam Child       Child model
    * @return             ModelSet of relation
    */
  def getModelSet[ParentTable <: ModelTable[Parent], Parent <: Model,
                  ChildTable <: ModelTable[Child], Child <: Model]
                  (childClass: Class[Child], parentRef: ChildTable => Rep[Int], parent: Parent):
                  ModelSet[ParentTable, Parent, ChildTable, Child]
  = new ModelSet[ParentTable, Parent, ChildTable, Child](childClass, parentRef, parent)

  /** Allows application of logical operators on model filters. */
  case class ModelFilter[T <: ModelTable[M], M <: Model](fn: T => Rep[Boolean]) {
    /** Applies && to the wrapped function */
    def &&(fn2: T => Rep[Boolean]): ModelFilter[T, M] = ModelFilter(m => trueIfNull(fn)(m) && trueIfNull(fn2)(m))
    /** Applies && to the specified filter's wrapped function */
    def +&(filter: ModelFilter[T, M]): ModelFilter[T, M] = this && filter.fn
    /** Applies || to the wrapped function */
    def ||(fn2: T => Rep[Boolean]): ModelFilter[T, M] = ModelFilter(m => falseIfNull(fn)(m) || falseIfNull(fn2)(m))
    /** Applies || to the specified filter's wrapped function */
    def +|(filter: ModelFilter[T, M]): ModelFilter[T, M] = this || filter.fn
    private def trueIfNull(fn: T => Rep[Boolean]): T => Rep[Boolean] = if (fn == null) _ => true else fn
    private def falseIfNull(fn: T => Rep[Boolean]): T => Rep[Boolean] = if (fn == null) _ => false else fn
  }

  implicit def filterToFunction[T <: ModelTable[M], M <: Model](filter: ModelFilter[T, M]): T => Rep[Boolean]
  = if (filter == null) null else filter.fn
  implicit def functionToFilter[T <: ModelTable[M], M <: Model](fn: T => Rep[Boolean]): ModelFilter[T, M] = ModelFilter(fn)

}
