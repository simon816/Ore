package db

import java.sql.Timestamp
import java.util.Date

import com.google.inject.ImplementedBy
import db.action.ModelAction._
import db.action.ModelFilter.IdFilter
import db.action.{AbstractModelAction, ModelActions}
import db.impl.OreModelService
import db.impl.OrePostgresDriver.api._
import db.meta.BindingsGenerator
import db.meta.TypeSetters._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.lifted.ColumnOrdered
import util.Conf
import util.Conf.debug

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[OreModelService])
trait ModelService {

  val bindingsGenerator: BindingsGenerator
  val registrar: ModelRegistrar
  import registrar.registerSetter

  protected[db] val DB: DatabaseConfig[JdbcProfile]

  registerSetter(classOf[Int], IntTypeSetter)
  registerSetter(classOf[String], StringTypeSetter)
  registerSetter(classOf[Boolean], BooleanTypeSetter)
  registerSetter(classOf[List[Int]], IntListTypeSetter)
  registerSetter(classOf[Timestamp], TimestampTypeSetter)

  def theTime: Timestamp = new Timestamp(new Date().getTime)

  /**
    * The default timeout when awaiting a query result.
    */
  val DefaultTimeout: Duration

  /**
    * Provides the specified ModelQueries granted that it is registered.
    *
    * @tparam Q ModelQueries
    * @return ModelQueries of type
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
    * Runs the specified action on the DB.
    *
    * @param action   Action to run
    * @return         Result
    */
  def process[R](action: AbstractModelAction[R]): Future[R] = DB.db.run(action).andThen {
    case r => action.processResult(this, r.get)
  }

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
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def insert[T <: ModelTable[M], M <: Model[_]](model: M): Future[M] = {
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
  def find[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M],
                                           predicate: T => Rep[Boolean]): Future[Option[M]] = {
    debug("Finding model of type " + modelClass)
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
  def get[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], id: Int,
                                          filter: T => Rep[Boolean] = null): Future[Option[M]]
  = find[T, M](modelClass, IdFilter[T, M](id) && filter)

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], id: Int): Future[Option[M]]
  = this.get(modelClass, id, null)

  /**
    * Returns a collection of models with the specified limit and offset.
    *
    * @param limit  Amount of models to take
    * @param offset Offset to drop
    * @return       Collection of models
    */
  def collect[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
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
  def filter[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
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
  def sorted[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M], sort: T => ColumnOrdered[_],
                                                limit: Int = -1, offset: Int = -1): Future[Seq[M]]
  = collect(modelClass, null, sort, limit, offset)

}
