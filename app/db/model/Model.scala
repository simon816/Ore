package db.model

import java.sql.Timestamp

import db.dao.{ModelFilter, ModelSet}
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import util.Conf._
import util.StringUtils

import scala.concurrent.Future

/**
  * Represents a Model that may or may not exist in the database.
  */
abstract class Model(val id: Option[Int], val createdAt: Option[Timestamp]) { self =>

  type M <: Model { type M = self.M }
  type T <: ModelTable[M]

  private var fieldBindings: Map[String, FieldBinding[M, _]] = Map.empty
  private var manyBindings: Map[Class[_ <: Model], ManyBinding] = Map.empty

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
  def get[A](key: String): Option[A] = {
    this.fieldBindings.get(key).map(_.asInstanceOf[FieldBinding[M, A]].valueFunc(this.asInstanceOf[M]))
  }

  /**
    * Marks the specified model class as a child of this model.
    *
    * @param childClass   Child model class
    * @param ref          Reference column to this model in child table
    */
  def bindMany(childClass: Class[_ <: Model], ref: ModelTable[_] => Rep[Int]) = {
    debug("Binding child " + childClass + " to model " + this)
    this.manyBindings += childClass -> ManyBinding(childClass, ref)
  }

  /**
    * Returns a [[ModelSet]] of the children for the specified child class.
    *
    * @param modelClass  Model class
    * @tparam Many       Child
    * @return            Set of children
    */
  def getMany[ManyTable <: ModelTable[Many], Many <: Model](modelClass: Class[Many]) =  Defined {
    val binding = this.manyBindings
      .find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("No child binding found for model " + modelClass + " in model " + this))._2
    new ModelSet[ManyTable, Many](modelClass, ModelFilter(binding.ref(_) === this.id.get))
  }

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

  protected def Defined[A](f: => A): A = {
    if (isDefined) f else throw new IllegalStateException("model must exist")
  }

}
