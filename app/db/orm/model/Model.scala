package db.orm.model

import java.sql.Timestamp
import java.text.SimpleDateFormat

import db.query.Queries
import models.project.Channel
import util.C._

import scala.concurrent.Future

/**
  * Represents a Model in the Database.
  */
abstract class Model { self =>

  type M <: Model { type M = self.M }

  /**
    * The format used for displaying dates for models.
    */
  val DateFormat = new SimpleDateFormat(OreConf.getString("date-format").get)

  private var tableBindings: Map[String, TableBinding[_]] = Map.empty

  private case class TableBinding[A](valueFunc: M => A, updateFunc: A => Seq[Future[_]])

  /**
    * Binds a new update function to the specified field name.
    *
    * @param key  Field name
    * @param f    Update function
    */
  def bind[A](key: String, value: M => A, f: A => Seq[Future[_]]) = {
    this.tableBindings += key -> TableBinding[A](value, f)
  }

  /**
    * Updates the specified key in the table.
    *
    * @param key  Binding key
    * @tparam A   Value type
    */
  def update[A](key: String) = {
    val binding = this.tableBindings.get(key).get.asInstanceOf[TableBinding[A]]
    val value = binding.valueFunc(this.asInstanceOf[M])
    for (f <- binding.updateFunc(value)) Queries.now(f).get
  }

  /**
    * Returns the value of the specified key.
    *
    * @param key  Model key
    * @tparam A   Value type
    * @return     Value of key
    */
  def get[A](key: String): A = {
    this.tableBindings.get(key).get.asInstanceOf[TableBinding[A]].valueFunc(this.asInstanceOf[M])
  }

  /**
    * Model ID
    *
    * @return ID of model
    */
  def id: Option[Int]

  /**
    * The Timestamp instant that this Model was created.
    *
    * @return Instant of creation
    */
  def createdAt: Option[Timestamp]

  /**
    * Returns a presentable date string of this models's creation date.
    *
    * @return Creation date string
    */
  def prettyDate: String = DateFormat.format(this.createdAt.get)

  /**
    * Returns true if this Project is defined in the database.
    *
    * @return True if defined in database
    */
  def isDefined: Boolean = this.id.isDefined

  /**
    * Returns a copy of this model with an updated ID and timestamp.
    *
    * @param id       ID to set
    * @param theTime  Timestamp
    * @return         Copy of model
    */
  def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model

  protected def assertDefined[A](f: => A): A = {
    if (isDefined) f else throw new IllegalStateException("model must exist")
  }

  implicit def convert: M = this.asInstanceOf[M]


}
