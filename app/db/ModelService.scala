package db

import java.sql.Timestamp
import java.util.Date

import db.action.ModelAction._
import db.action.ModelActions
import db.action.ModelFilter.IdFilter
import db.meta.BootstrapTypeSetters._
import db.meta.ModelProcessor
import slick.backend.DatabaseConfig
import slick.driver.{JdbcDriver, JdbcProfile}
import slick.lifted.ColumnOrdered

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

/**
  * Represents a service that creates, deletes, and manipulates Models.
  */
trait ModelService {

  /** Used for processing models and determining field bindings */
  val processor: ModelProcessor

  /** All registered models and TypeSetters */
  val registrar: ModelRegistrar
  import registrar.registerSetter

  /** The base JDBC driver */
  val driver: JdbcDriver
  import driver.api._

  /**
    * The database config for raw actions. Note: running raw queries will not
    * process any returned models and should be used only for model "meta-data"
    * (e.g. Project stars).
    */
  protected[db] val DB: DatabaseConfig[JdbcProfile]

  // Bootstrap TypeSetters
  registerSetter(classOf[Int], IntTypeSetter)
  registerSetter(classOf[String], StringTypeSetter)
  registerSetter(classOf[Boolean], BooleanTypeSetter)
  registerSetter(classOf[Timestamp], TimestampTypeSetter)
  registerSetter(classOf[List[Int]], IntListTypeSetter)
  registerSetter(classOf[List[String]], StringListTypeSetter)
  registerSetter(classOf[List[Boolean]], BooleanListTypeSetter)
  registerSetter(classOf[List[Timestamp]], TimestampListTypeSetter)

  /**
    * Returns a current Timestamp.
    *
    * @return Timestamp of now
    */
  def theTime: Timestamp = new Timestamp(new Date().getTime)

  /**
    * The default timeout when awaiting a query result.
    */
  val DefaultTimeout: Duration

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f        Future to await
    * @param timeout  Timeout duration
    * @tparam M       Return type
    * @return         Try of return type
    */
  def await[M](f: Future[M], timeout: Duration = DefaultTimeout): Try[M] = Await.ready(f, timeout).value.get

  /**
    * Provides the specified ModelActions granted that it is registered.
    *
    * @tparam Q ModelActions
    * @return ModelActions of type
    */
  def provide[Q <: ModelActions[_, _]](actionsClass: Class[Q]): Q = this.registrar.reverseLookup(actionsClass)

  /**
    * Returns the base query for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam T         Table type
    * @tparam M         Model type
    * @return           Base query for Model
    */
  def newModelAction[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M]): Query[T, M, Seq]
  = this.registrar.get(modelClass).baseQuery.asInstanceOf[Query[T, M, Seq]]

  /**
    * Runs the specified ModelAction on the DB and processes the resulting
    * model(s).
    *
    * @param action   Action to run
    * @return         Processed result
    */
  def process[R: TypeTag](action: AbstractModelAction[R]): Future[R] = DB.db.run(action).map {
    case r => action.processResult(this, r)
  }

  /**
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def insert[T <: ModelTable[M], M <: Model[_]: TypeTag](model: M): Future[M] = {
    val toInsert = model.copyWith(None, Some(theTime)).asInstanceOf[M]
    val models = newModelAction[T, M](model.getClass)
    process {
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
  def find[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M],
                                                       predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val modelPromise = Promise[Option[M]]
    val query = newModelAction[T, M](modelClass).filter(predicate).take(1)
    process(query.result).andThen {
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
  def count[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], filter: T => Rep[Boolean]): Future[Int] = {
    var query = newModelAction[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    DB.db.run(query.length.result)
  }

  /**
    * Returns the size of the model table.
    *
    * @return Size of model table
    */
  def count[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M]): Future[Int] = this.count(modelClass, null)

  /**
    * Deletes the specified Model.
    *
    * @param model Model to delete
    */
  def delete[T <: ModelTable[M], M <: Model[_]](model: M, filter: T => Rep[Boolean] = null): Future[Int]
  = DB.db.run(newModelAction[T, M](model.getClass).filter(IdFilter[T, M](model.id.get) && filter).delete)

  /**
    * Deletes the specified Model.
    *
    * @param model Model to delete
    */
  def delete[T <: ModelTable[M], M <: Model[_]](model: M): Future[Int] = this.delete(model, null)

  /**
    * Deletes all the models meeting the specified filter.
    *
    * @param modelClass Model class
    * @param filter     Filter to use
    * @tparam T         Table
    * @tparam M         Model
    */
  def deleteWhere[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], filter: T => Rep[Boolean]): Future[Int]
  = DB.db.run(newModelAction[T, M](modelClass).filter(filter).delete)

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M], id: Int,
                                          filter: T => Rep[Boolean] = null): Future[Option[M]]
  = find[T, M](modelClass, IdFilter[T, M](id) && filter)

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M], id: Int): Future[Option[M]]
  = this.get(modelClass, id, null)

  /**
    * Returns a collection of models with the specified limit and offset.
    *
    * @param limit  Amount of models to take
    * @param offset Offset to drop
    * @return       Collection of models
    */
  def collect[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
                                                          sort: T => ColumnOrdered[_],
                                                          limit: Int, offset: Int): Future[Seq[M]] = {
    var query = newModelAction[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    if (sort != null) query = query.sortBy(sort)
    if (offset > -1) query = query.drop(offset)
    if (limit > -1) query = query.take(limit)
    process(query.result)
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
  def filter[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
                                                         limit: Int = -1, offset: Int = -1): Future[Seq[M]]
  = collect(modelClass, filter, null, limit, offset)

  /**
    * Sorts the models by the specified ColumnOrdered.
    *
    * @param modelClass Model class
    * @param sort       Ordering
    * @param limit      Amount to take
    * @param offset     Amount to drop
    * @tparam T         Model table
    * @tparam M         Model
    * @return           Sorted models
    */
  def sorted[T <: ModelTable[M], M <: Model[_]: TypeTag](modelClass: Class[_ <: M], sort: T => ColumnOrdered[_],
                                                         limit: Int = -1, offset: Int = -1): Future[Seq[M]]
  = collect(modelClass, null, sort, limit, offset)

}
