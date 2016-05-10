package db

import java.sql.Timestamp

import db.action.{ModelActions, ModelFilter, ModelAccess}
import db.meta.{Actor, FieldBinding, ManyBinding}
import slick.driver.JdbcDriver
import util.{OreConfig, StringUtils}

import scala.concurrent.Future

/**
  * Represents a Model that may or may not exist in the database.
  */
abstract class Model(val id: Option[Int], val createdAt: Option[Timestamp], val driver: JdbcDriver) { self =>

  import driver.api._

  type M <: Model { type M = self.M }
  type T <: ModelTable[M]
  type A <: ModelActions[T, M]

  /** The ModelService that this Model was processed with */
  implicit var service: ModelService = null

  private var _isProcessed = false
  private var fieldBindings: Map[String, FieldBinding[M, _]] = Map.empty
  private var manyBindings: Map[Class[_ <: Model], ManyBinding] = Map.empty

  /**
    * Returns the ModelActions associated with this Model.
    *
    * @param service  Optional model service to provide to the model if it has
    *                 not yet been processed
    * @return         ModelActions
    */
  def actions(implicit service: ModelService = null): A = {
    if (this.service == null) this.service = service
    println("class " + this.getClass)
    println("annotation " + getClass.getAnnotation(classOf[Actor]))
    service.provide(this.getClass.getAnnotation(classOf[Actor]).value.asInstanceOf[Class[A]])
  }

  /**
    * Binds a new update function to the specified field name.
    *
    * @param key  Field name
    * @param f    Update function
    */
  def bind[R](key: String, value: M => R, f: R => Future[_])
  = this.fieldBindings += key -> FieldBinding[M, R](value, f)

  /**
    * Updates the specified key in the table.
    *
    * @param key  Binding key
    * @tparam R   Value type
    */
  def update[R](key: String) = {
    val binding = this.fieldBindings
      .getOrElse(key, throw new RuntimeException("No field binding found for key " + key + " in model " + this))
      .asInstanceOf[FieldBinding[M, R]]
    val value = binding.valueFunc(this.asInstanceOf[M])
    service.await(binding.updateFunc(value)).get
  }

  /**
    * Returns the value of the specified key.
    *
    * @param key  Model key
    * @tparam R   Value type
    * @return     Value of key
    */
  def get[R](key: String): Option[R]
  = this.fieldBindings.get(key).map(_.asInstanceOf[FieldBinding[M, R]].valueFunc(this.asInstanceOf[M]))

  /**
    * Marks the specified model class as a child of this model.
    *
    * @param childClass   Child model class
    * @param ref          Reference column to this model in child table
    */
  def bindMany(childClass: Class[_ <: Model], ref: ModelTable[_] => Rep[Int])
  = this.manyBindings += childClass -> ManyBinding(childClass, ref)

  /**
    * Returns a [[ModelAccess]] of the children for the specified child class.
    *
    * @param modelClass  Model class
    * @tparam Many       Child
    * @return            Set of children
    */
  def getMany[ManyTable <: ModelTable[Many], Many <: Model](modelClass: Class[Many]) = Defined {
    val binding = this.manyBindings
      .find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("No child binding found for model " + modelClass + " in model " + this))._2
    this.service.access[ManyTable, Many](modelClass, ModelFilter(binding.ref(_) === this.id.get))
  }

  /**
    * Returns a presentable date string of this models's creation date.
    *
    * @return Creation date string
    */
  def prettyDate(implicit config: OreConfig): String = StringUtils.prettyDate(this.createdAt.get)

  /**
    * Returns true if this Project is defined in the database.
    *
    * @return True if defined in database
    */
  def isDefined: Boolean = this.id.isDefined

  /**
    * Removes this model from it's table through it's ModelActions.
    */
  def remove() = this.actions.delete(this.asInstanceOf[M])

  /**
    * Returns a copy of this model with an updated ID and timestamp.
    *
    * @param id       ID to set
    * @param theTime  Timestamp
    * @return         Copy of model
    */
  def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model

  /**
    * Returns true if this model has been processed internally by some
    * ModelService and has had it's bindings processed.
    *
    * @return True if processed
    */
  def isProcessed: Boolean = this._isProcessed

  protected[db] def setProcessed(processed: Boolean) = this._isProcessed = processed

  protected def Defined[R](f: => R): R = {
    if (isDefined) f else throw new IllegalStateException("model must exist")
  }

}
