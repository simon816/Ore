package db.model

import java.sql.Timestamp

import db.dao.ChildModelSet
import db.driver.OrePostgresDriver.api._
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

  private var fieldBindings: Map[String, FieldBinding[M, _]] = Map.empty
  private var childBindings: Map[Class[_ <: Model], ChildBinding[_, _]] = Map.empty

  implicit def convertSelf(self: Model): M = self.asInstanceOf[M]

  /**
    * Binds a new update function to the specified field name.
    *
    * @param key  Field name
    * @param f    Update function
    */
  def bind[A](key: String, value: M => A, f: A => Seq[Future[_]]) = {
    debug("Binding key " + key + " to model " + this)
    this.fieldBindings += key -> FieldBinding[M, A](value, f)
  }

  /**
    * Updates the specified key in the table.
    *
    * @param key  Binding key
    * @tparam A   Value type
    */
  def update[A](key: String) = {
    val binding = this.fieldBindings
      .getOrElse(key, throw new RuntimeException("No field binding found for key " + key + " in model " + this))
      .asInstanceOf[FieldBinding[M, A]]
    val value = binding.valueFunc(this)
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
  def get[A](key: String): Option[A] = {
    this.fieldBindings.get(key).map(_.asInstanceOf[FieldBinding[M, A]].valueFunc(this))
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
                                                                 ref: ChildTable => Rep[Int]) = {
    debug("Binding child " + childClass + " to model " + this)
    this.childBindings += childClass -> ChildBinding[ChildTable, Child](childClass, ref)
  }

  /**
    * Returns a [[ChildModelSet]] of the children for the specified child class.
    *
    * @param modelClass   Model class
    * @tparam ChildTable  Child table
    * @tparam Child       Child
    * @return             Set of children
    */
  def getChildren[ChildTable <: ModelTable[Child], Child <: Model](modelClass: Class[_ <: Child]) = {
    val binding = this.childBindings
      .find(_._1.isAssignableFrom(modelClass))
      .map(_._2.asInstanceOf[ChildBinding[ChildTable, Child]])
      .getOrElse(throw new RuntimeException("No child binding found for model " + modelClass + " in model " + this))
    new ChildModelSet[T, M, ChildTable, Child](binding.childClass.asInstanceOf[Class[Child]], binding.ref, this)
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
