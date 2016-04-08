package db.query

import java.sql.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

import db.OrePostgresDriver.api._
import db._
import models.auth.User
import models.project.{Channel, Page, Project, Version}
import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * Contains all queries for retrieving models from the database.
  */
object Queries {

  val Users     =   UserQueries
  val Projects  =   ProjectQueries
  val Channels  =   ChannelQueries
  val Versions  =   VersionQueries
  val Pages     =   PageQueries

  /**
    * The default timeout when awaiting a query result.
    */
  val DEFAULT_TIMEOUT: Duration = Duration(10, TimeUnit.SECONDS)

  /**
    * Awaits the result of the specified future and returns the result.
    *
    * @param f        Future to await
    * @param timeout  Timeout duration
    * @tparam M       Return type
    * @return         Try of return type
    */
  def now[M](f: Future[M], timeout: Duration = DEFAULT_TIMEOUT): Try[M] = {
    Await.ready(f, timeout).value.get
  }

  protected[db] val Config = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  protected[db] val DB = Config.db

  protected[db] def theTime: Timestamp = new Timestamp(new Date().getTime)

  protected[db] def q[T <: Table[_]](clazz: Class[_]): TableQuery[T] = {
    // Table mappings
    if (classOf[Project].isAssignableFrom(clazz)) {
      TableQuery(tag => new ProjectTable(tag).asInstanceOf[T])
    } else if (classOf[Project].isAssignableFrom(clazz)) {
      TableQuery(tag => new TeamTable(tag).asInstanceOf[T])
    } else if (classOf[Channel].isAssignableFrom(clazz)) {
      TableQuery(tag => new ChannelTable(tag).asInstanceOf[T])
    } else if (classOf[Version].isAssignableFrom(clazz)) {
      TableQuery(tag => new VersionTable(tag).asInstanceOf[T])
    } else if (classOf[User].isAssignableFrom(clazz)) {
      TableQuery(tag => new UserTable(tag).asInstanceOf[T])
    } else if (classOf[Page].isAssignableFrom(clazz)) {
      TableQuery(tag => new PagesTable(tag).asInstanceOf[T])
    } else {
      throw new Exception("No table found for class: " + clazz.toString)
    }
  }

  protected[db] def filterQuery[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]) = {
    // Raw filter query
    q[T](clazz).filter(predicate)
  }

  protected[db] def filter[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Seq[M]] = {
    // Filter action
    DB.run(filterQuery[T, M](clazz, predicate).result)
  }

  protected[db] def find[T <: Table[M], M](clazz: Class[_], predicate: T => Rep[Boolean]): Future[Option[M]] = {
    val p = Promise[Option[M]]
    filter[T, M](clazz, predicate).onComplete {
      case Failure(thrown) => p.failure(thrown)
      case Success(m) => p.success(m.headOption)
    }
    p.future
  }

}
