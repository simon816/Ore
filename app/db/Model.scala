package db

import java.sql.Timestamp

import com.google.common.base.Preconditions.checkNotNull
import db.action.{ModelAccess, ModelActions, ModelFilter}
import db.impl.pg.OrePostgresDriver.api._
import db.meta.{Actions, FieldBinding, ManyToManyBinding, OneToManyBinding}

import scala.concurrent.{Future, Promise}

/**
  * Represents a Model that may or may not exist in the database.
  */
abstract class Model(val id: Option[Int], val createdAt: Option[Timestamp]) { self =>

  /** Self referential type */
  type M <: Model { type M = self.M }
  /** The model's table */
  type T <: ModelTable[M]
  /** The model's actions */
  type A <: ModelActions[T, M]

  /** The ModelService that this Model was processed with */
  implicit var service: ModelService = _

  private var _isProcessed = false
  private var fieldBindings: Map[String, FieldBinding[M, _]] = Map.empty
  private var oneToManyBindings: Map[Class[_ <: Model], OneToManyBinding] = Map.empty
  private var manyToManyBindings: Map[Class[_ <: Model], ManyToManyBinding[_]] = Map.empty

  /**
    * Returns the ModelActions associated with this Model.
    *
    * @param service  Optional model service to provide to the model if it has
    *                 not yet been processed
    * @return         ModelActions
    */
  def actions(implicit service: ModelService = null): A = {
    if (this.service == null)
      this.service = service
    checkNotNull(this.service, "service is null", "")
    val clazz = this.getClass
    if (!clazz.isAnnotationPresent(classOf[Actions]))
      throw new RuntimeException("missing @Actions annotation")
    val actions = this.service.getActions(clazz.getAnnotation(classOf[Actions]).value)
    if (!actions.isInstanceOf[A])
      throw new RuntimeException("model actions class does not match type")
    actions.asInstanceOf[A]
  }

  /**
    * Binds a new update function to the specified field name.
    *
    * @param key  Field name
    * @param f    Update function
    */
  def bind[R](key: String, value: M => R, f: R => Future[_]) = {
    checkNotNull(key, "key is null", "")
    checkNotNull(value, "value function is null", "")
    checkNotNull(f, "update function is null", "")
    this.fieldBindings += key -> FieldBinding[M, R](value, f)
  }

  /**
    * Updates the specified key in the table.
    *
    * @param key  Binding key
    * @tparam R   Value type
    */
  def update[R](key: String) = {
    checkNotNull(key, "key is null", "")
    val binding = this.fieldBindings
      .getOrElse(key, throw new RuntimeException("No field binding found for key " + key + " in model " + this))
      .asInstanceOf[FieldBinding[M, R]]
    val value = binding.getValue(this.asInstanceOf[M])
    this.service.await(binding.setValue(value)).get
  }

  /**
    * Returns the value of the specified key.
    *
    * @param key  Model key
    * @tparam R   Value type
    * @return     Value of key
    */
  def get[R](key: String): Option[R] = {
    checkNotNull(key, "key is null", "")
    this.fieldBindings.get(key).map(_.asInstanceOf[FieldBinding[M, R]].getValue(this.asInstanceOf[M]))
  }

  /**
    * Marks the specified model class as a child of this model.
    *
    * @param childClass   Child model class
    * @param ref          Reference column to this model in child table
    */
  def bindOneToMany(childClass: Class[_ <: Model], ref: ModelTable[_] => Rep[Int]) = {
    checkNotNull(childClass, "child class is null", "")
    checkNotNull(ref, "parent reference is null", "")
    this.oneToManyBindings += childClass -> OneToManyBinding(childClass, ref)
  }

  /**
    * Returns a [[ModelAccess]] of the children for the specified child class.
    *
    * @param modelClass  Model class
    * @tparam Many       Child
    * @return            Set of children
    */
  def oneToMany[ManyTable <: ModelTable[Many], Many <: Model](modelClass: Class[Many]) = Defined {
    checkNotNull(modelClass, "model class is null", "")
    val binding = this.oneToManyBindings
      .find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("No child binding found for model " + modelClass + " in model " + this))._2
    this.service.access[ManyTable, Many](modelClass, ModelFilter(binding.ref(_) === this.id.get))
  }

  /**
    * Binds a many-to-many relationship. Internally these are represented by a
    * mediator table that tracks relationships between two models.
    *
    * @param childClass     Model class to bind to
    * @param relationTable  A TableQuery instance of the relations table
    * @param selfRef        How the relations table references this model
    * @param otherRef       How the relations table references the target model
    * @param finalRef       The primary key field within the target ModelTable
    * @tparam RelationTable Relation table type
    */
  def bindManyToMany[RelationTable <: Table[_]](childClass: Class[_ <: Model],
                                                relationTable: TableQuery[_],
                                                selfRef: RelationTable => Rep[Int],
                                                otherRef: RelationTable => Rep[Int],
                                                finalRef: ModelTable[_] => Rep[Int]) = {
    checkNotNull(childClass, "child class is null", "")
    checkNotNull(relationTable, "relation table is null", "")
    this.manyToManyBindings += childClass -> ManyToManyBinding(childClass, relationTable, selfRef, otherRef, finalRef)
  }

  /**
    * Returns ModelAccess to a bound many-to-many relationship Model.
    * Internally this is handled by finding all rows in the mediator table that
    * have a reference to this model's ID and then using the target model's ID
    * in that same row within the target model's ModelTable.
    *
    * @param modelClass     Target model class
    * @tparam RelationTable Mediator table
    * @tparam ManyTable     Target model table
    * @tparam Many          Target model
    * @return               ModelAccess to target model
    */
  def manyToMany[RelationTable <: Table[_], ManyTable <: ModelTable[Many], Many <: Model](modelClass: Class[Many]) = {
    Defined {
      checkNotNull(modelClass, "model class is null", "")

      val binding = this.manyToManyBindings
        .find(_._1.isAssignableFrom(modelClass))
        .getOrElse(throw new RuntimeException("No child binding found for model " + modelClass + " in model " + this))
        ._2.asInstanceOf[ManyToManyBinding[RelationTable]]

      // Find all entries in relations table that match the ID of this model
      val promise: Promise[ModelAccess[ManyTable, Many]] = Promise()
      this.service.DB.db.run {
        (for (relation <- binding.table.filter(binding.selfRef(_) === this.id.get))
          yield binding.otherRef(relation.asInstanceOf[RelationTable])).result
      } andThen {
        case manyIds =>
          promise.success {
            this.service.access[ManyTable, Many](modelClass, ModelFilter(binding.finalRef(_) inSetBind manyIds.get))
          }
      }

      promise.future
    }
  }

  /**
    * Returns true if this Project is defined in the database.
    *
    * @return True if defined in database
    */
  def isDefined: Boolean = this.id.isDefined

  /**
    * Removes this model from it's table through it's ModelActions.
    */
  def remove() = Defined(this.service.await(this.actions.delete(this.asInstanceOf[M])))

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
    if (isDefined)
      f
    else
      throw new IllegalStateException("model must exist")
  }

}
