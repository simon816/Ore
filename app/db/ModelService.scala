package db

import java.sql.Timestamp
import java.util.Date

import db.action.ModelAction._
import db.action.ModelFilter.IdFilter
import db.action.{ModelAccess, ModelActions, ModelFilter}
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

  /** All registered models and [[db.meta.TypeSetter]]s */
  val registry: ModelRegistry
  import registry.registerTypeSetter

  /** The base JDBC driver */
  val driver: JdbcDriver
  import driver.api._

  /**
    * The default timeout when awaiting a query result.
    */
  val DefaultTimeout: Duration

  /**
    * The database config for raw actions. Note: running raw queries will not
    * process any returned models and should be used only for model "meta-data"
    * (e.g. Project stars).
    */
  protected[db] val DB: DatabaseConfig[JdbcProfile]

  // Bootstrap TypeSetters
  registerTypeSetter(classOf[Int], IntTypeSetter)
  registerTypeSetter(classOf[String], StringTypeSetter)
  registerTypeSetter(classOf[Boolean], BooleanTypeSetter)
  registerTypeSetter(classOf[Timestamp], TimestampTypeSetter)
  registerTypeSetter(classOf[List[Int]], IntListTypeSetter)
  registerTypeSetter(classOf[List[String]], StringListTypeSetter)
  registerTypeSetter(classOf[List[Boolean]], BooleanListTypeSetter)
  registerTypeSetter(classOf[List[Timestamp]], TimestampListTypeSetter)

  /**
    * Returns a current Timestamp.
    *
    * @return Timestamp of now
    */
  def theTime: Timestamp = new Timestamp(new Date().getTime)

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
  def getActions[Q <: ModelActions[_, _]](actionsClass: Class[Q]): Q = this.registry.getActions(actionsClass)

  /**
    * Provides the registered ModelActions instance for the specified Model.
    *
    * @param modelClass Model class
    * @tparam T Table type
    * @tparam M Model type
    * @return ModelActions
    */
  def getActionsByModel[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): ModelActions[T, M]
  = this.registry.getActionsByModel(modelClass)

  /**
    * Returns the base query for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam T         Table type
    * @tparam M         Model type
    * @return           Base query for Model
    */
  def newAction[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): Query[T, M, Seq]
  = this.registry.getActionsByModel(modelClass).baseQuery.asInstanceOf[Query[T, M, Seq]]

  /**
    * Runs the specified ModelAction on the DB and processes the resulting
    * model(s).
    *
    * @param action   Action to run
    * @return         Processed result
    */
  def doAction[R: TypeTag](action: AbstractModelAction[R]): Future[R]
  = DB.db.run(action).map(r => action.processResult(this, r))

  /**
    * Returns a new ModelAccess to access a ModelTable synchronously.
    *
    * @param modelClass Model class
    * @param baseFilter Base filter to apply
    * @tparam T         Model table
    * @tparam M         Model
    * @return           New ModelAccess
    */
  def access[T <: ModelTable[M], M <: Model](modelClass: Class[M], baseFilter: ModelFilter[T, M] = ModelFilter[T, M]())
  = new ModelAccess[T, M](this, modelClass, baseFilter)

  /**
    * Returns the specified [[ModelBase]].
    *
    * @param clazz  ModelBase class
    * @tparam B     ModelBase type
    * @return       ModelBase
    */
  def access[B <: ModelBase[_, _]](clazz: Class[B]): B = this.registry.getModelBase(clazz)

  /**
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def insert[T <: ModelTable[M], M <: Model: TypeTag](model: M): Future[M] = {
    val toInsert = model.copyWith(None, Some(theTime)).asInstanceOf[M]
    val models = newAction[T, M](model.getClass)
    doAction {
      models returning models.map(_.id) into {
        case (m, id) =>
          model.copyWith(Some(id), m.createdAt).asInstanceOf[M]
      } += toInsert
    }
  }

  /**
    * Returns the first model that matches the given predicate.
    *
    * @param filter  Filter
    * @return        Optional result
    */
  def find[T <: ModelTable[M], M <: Model: TypeTag](modelClass: Class[_ <: M],
                                                    filter: T => Rep[Boolean]): Future[Option[M]] = {
    val modelPromise = Promise[Option[M]]
    val query = newAction[T, M](modelClass).filter(filter).take(1)
    doAction(query.result).andThen {
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
    var query = newAction[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    DB.db.run(query.length.result)
  }

  /**
    * Deletes the specified Model.
    *
    * @param model Model to delete
    */
  def delete[T <: ModelTable[M], M <: Model](model: M): Future[Int]
  = DB.db.run(newAction[T, M](model.getClass).filter(IdFilter[T, M](model.id.get)).delete)

  /**
    * Deletes all the models meeting the specified filter.
    *
    * @param modelClass Model class
    * @param filter     Filter to use
    * @tparam T         Table
    * @tparam M         Model
    */
  def deleteWhere[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M], filter: T => Rep[Boolean]): Future[Int]
  = DB.db.run(newAction[T, M](modelClass).filter(filter).delete)

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get[T <: ModelTable[M], M <: Model: TypeTag](modelClass: Class[_ <: M], id: Int,
                                                   filter: T => Rep[Boolean] = null): Future[Option[M]]
  = find[T, M](modelClass, IdFilter[T, M](id) && filter)

  /**
    * Returns a collection of models with the specified limit and offset.
    *
    * @param limit  Amount of models to take
    * @param offset Offset to drop
    * @return       Collection of models
    */
  def collect[T <: ModelTable[M], M <: Model: TypeTag](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
                                                       sort: T => ColumnOrdered[_], limit: Int,
                                                       offset: Int): Future[Seq[M]] = {
    var query = newAction[T, M](modelClass)
    if (filter != null) query = query.filter(filter)
    if (sort != null) query = query.sortBy(sort)
    if (offset > -1) query = query.drop(offset)
    if (limit > -1) query = query.take(limit)
    doAction(query.result)
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
  def filter[T <: ModelTable[M], M <: Model: TypeTag](modelClass: Class[_ <: M], filter: T => Rep[Boolean],
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
  def sorted[T <: ModelTable[M], M <: Model: TypeTag](modelClass: Class[_ <: M], sort: T => ColumnOrdered[_],
                                                      filter: T => Rep[Boolean] = null, limit: Int = -1,
                                                      offset: Int = -1): Future[Seq[M]]
  = collect(modelClass, filter, sort, limit, offset)

}
