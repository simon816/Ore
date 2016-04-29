package db.orm.model

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.dao.ChildModelSet
import db.query.ModelQueries
import util.C._
import util.StringUtils

import scala.concurrent.Future

/**
  * Represents a Model in the Database.
  */
abstract class Model { self =>

  type M <: Model { type M = self.M }
  type T <: ModelTable[M]

  private var tableBindings: Map[String, TableBinding[_]] = Map.empty
  private case class TableBinding[A](valueFunc: M => A, updateFunc: A => Seq[Future[_]])

  private var childBindings: Map[Class[_ <: Model], ChildBinding[_, _]] = Map.empty
  private case class ChildBinding[ChildTable <: ModelTable[Child], Child <: Model](childClass: Class[_ <: Child],
                                                                                   ref: ChildTable => Rep[Int])

  /**
    * Binds a new update function to the specified field name.
    *
    * @param key  Field name
    * @param f    Update function
    */
  def bind[A](key: String, value: M => A, f: A => Seq[Future[_]])
  = this.tableBindings += key -> TableBinding[A](value, f)

  /**
    * Updates the specified key in the table.
    *
    * @param key  Binding key
    * @tparam A   Value type
    */
  def update[A](key: String) = {
    val binding = this.tableBindings.get(key).get.asInstanceOf[TableBinding[A]]
    val value = binding.valueFunc(this.asInstanceOf[M])
    debug("Updating key \"" + key + "\" in model " + getClass + " to " + value)
    for (f <- binding.updateFunc(value)) ModelQueries.await(f).get
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
    * Marks the specified model class as a child of this model.
    *
    * @param childClass   Child model class
    * @param ref          Reference column to this model in child table
    * @tparam ChildTable  Child table
    * @tparam Child       Child model
    */
  def bindChild[ChildTable <: ModelTable[Child], Child <: Model](childClass: Class[_ <: Child],
                                                                 ref: ChildTable => Rep[Int])
  = this.childBindings += childClass -> ChildBinding[ChildTable, Child](childClass, ref)

  /**
    * Returns a [[ChildModelSet]] of the children for the specified child class.
    *
    * @param modelClass   Model class
    * @tparam ChildTable  Child table
    * @tparam Child       Child
    * @return             Set of children
    */
  def getChildren[ChildTable <: ModelTable[Child], Child <: Model](modelClass: Class[_ <: Child]) = {
    val binding = this.childBindings.find(_._1.isAssignableFrom(modelClass)).get._2
      .asInstanceOf[ChildBinding[ChildTable, Child]]
    new ChildModelSet[T, M, ChildTable, Child](binding.childClass.asInstanceOf[Class[Child]], binding.ref,
      this.asInstanceOf[M])
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
  def prettyDate: String = StringUtils.prettyDate(this.createdAt.get)

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

}
