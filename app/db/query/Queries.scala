package db.query

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

import db.OrePostgresDriver.api._
import db._
import db.query.Queries.DB.run
import db.query.Queries._
import play.api.Play
import play.api.Play.current
import play.api.Play.{configuration => config}
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Base class for handling Model queries.
  *
  * @tparam T ModelTable
  * @tparam M Model
  */
abstract class Queries[T <: ModelTable[M], M <: Model](val models: TableQuery[T]) {

  /**
    * Sets an Int field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setInt(model: M, key: T => Rep[Int], value: Int) = {
    val query = for { m <- this.models if m.pk === model.id.get } yield key(m)
    run(query.update(value))
  }

  /**
    * Sets a String field on the Model.
    *
    * @param model  Model to update
    * @param key    Key to update
    * @param value  Value to set
    */
  def setString(model: M, key: T => Rep[String], value: String) = {
    val query = for { m <- this.models if m.pk === model.id.get } yield key(m)
    run(query.update(value))
  }

  /**
    * Sets an int array field on the Model.
    *
    * @param model  Model to update
    * @param pk     Primary key name
    * @param col    Column name
    * @param value  Value
    */
  def setIntList(model: M, pk: String, col: String, value: List[Int]) = {
    val tableName = models.baseTableRow.tableName
    val v = value.mkString("{", ",", "}")
    val action = sqlu"""update $tableName set $col = '$v' where $pk = ${model.id};"""
    run(action)
  }

  /**
    * Returns the first model that matches the given predicate.
    *
    * @param predicate  Predicate
    * @return           Optional result
    */
  def find(predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val modelPromise = Promise[Option[M]]
    val query = this.models.filter(predicate).take(1)
    run(query.result).andThen {
      case Failure(thrown) => modelPromise.failure(thrown)
      case Success(result) => modelPromise.success(result.headOption)
    }
    modelPromise.future
  }

  /**
    * Returns the model with the specified ID, if any.
    *
    * @param id   Model with ID
    * @return     Model if present, None otherwise
    */
  def get(id: Int): Future[Option[M]] = {
    find(m => m.pk === id)
  }

  /**
    * Returns a collection of models with the specified limit and offset.
    *
    * @param limit  Amount of models to take
    * @param offset Offset to drop
    * @return       Collection of models
    */
  def collect(limit: Int = -1, offset: Int = -1, filter: T => Rep[Boolean] = null): Future[Seq[M]] = {
    var query: Query[T, M, Seq] = this.models
    if (filter != null) query = query.filter(filter)
    if (offset > -1) query = query.drop(offset)
    if (limit > -1) query = query.take(limit)
    run(query.result)
  }

  /**
    * Creates the specified model in it's table.
    *
    * @param model  Model to create
    * @return       Newly created model
    */
  def create(model: M): Future[M] = {
    val toInsert = copyInto(None, Some(theTime), model)
    val query = {
      this.models returning this.models.map(_.pk) into {
        case (m, id) =>
          copyInto(Some(id), m.createdAt, m)
      } += toInsert
    }
    run(query)
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
        case Some(existing) => modelPromise.success(existing)
        case None => modelPromise.completeWith(create(model))
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
    val query = this.models.filter(m => m.pk === model.id.get)
    run(query.delete)
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

/**
  * Contains all queries for retrieving models from the database.
  */
object Queries {

  val Users     =   new UserQueries
  val Projects  =   new ProjectQueries
  val Channels  =   new ChannelQueries
  val Versions  =   new VersionQueries
  val Pages     =   new PageQueries

  /**
    * The default timeout when awaiting a query result.
    */
  val DefaultTimeout: Duration = Duration(config.getInt("application.db.default-timeout").get, TimeUnit.SECONDS)

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f        Future to await
    * @param timeout  Timeout duration
    * @tparam M       Return type
    * @return         Try of return type
    */
  def now[M](f: Future[M], timeout: Duration = DefaultTimeout): Try[M] = {
    Await.ready(f, timeout).value.get
  }

  protected[db] val Config = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  protected[db] val DB = Config.db

  protected[db] def theTime: Timestamp = new Timestamp(new Date().getTime)

}
