package db

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.collect.{BiMap, HashBiMap}

import scala.collection.JavaConverters._

/**
  * A registry for ModelActions. This contains all the necessary information to
  * interact with any Model in the database.
  */
trait ModelRegistry {

  private val modelActions: BiMap[Class[_ <: Model], ModelSchema[_]] = HashBiMap.create()
  private var modelBases: Map[Class[_ <: ModelBase[_]], ModelBase[_]] = Map.empty

  /**
    * Registers a new ModelActions.
    *
    * @param actions ModelActions to register
    * @tparam Q Type Actions type
    * @return Registered actions
    */
  //noinspection ComparingUnrelatedTypes
  def registerSchema[Q <: ModelSchema[_ <: Model]](actions: Q): Q = {
    checkNotNull(actions, "actions are null", "")
    this.modelActions.put(actions.modelClass, actions)
    actions
  }

  /**
    * Finds a ModelActions instance by the ModelActions class.
    *
    * @param actionsClass ModelActions class
    * @tparam Q           Actions type
    * @return             ModelActions
    */
  //noinspection ComparingUnrelatedTypes
  def getSchema[Q <: ModelSchema[_]](actionsClass: Class[Q]): Q = {
    checkNotNull(actionsClass, "actions class is null", "")
    this.modelActions.asScala.find(_._2.getClass.equals(actionsClass))
      .getOrElse(throw new RuntimeException("actions not found of type " + actionsClass))
      ._2.asInstanceOf[Q]
  }

  /**
    * Returns a registered ModelActions for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam M         Model type
    * @return           ModelActions of Model
    */
  def getSchemaByModel[M <: Model](modelClass: Class[_ <: M]): ModelSchema[M] = {
    checkNotNull(modelClass, "model class is null", "")
    this.modelActions.asScala.find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("actions not found for model " + modelClass))
      ._2.asInstanceOf[ModelSchema[M]]
  }

  /**
    * Registers a new [[ModelBase]] with the service.
    *
    * @param clazz  ModelBase class
    * @param base   ModelBase
    * @tparam B     Type
    */
  def registerModelBase[B <: ModelBase[_]](clazz: Class[B], base: B): B = {
    checkNotNull(clazz, "model class is null", "")
    checkNotNull(base, "model base is null", "")
    this.modelBases += clazz -> base
    base
  }

  /**
    * Returns a registered [[ModelBase]] by class.
    *
    * @param clazz  ModelBase class
    * @tparam B     ModelBase type
    * @return       ModelBase of class
    */
  def getModelBase[B <: ModelBase[_]](clazz: Class[B]): B = {
    checkNotNull(clazz, "model class is null", "")
    this.modelBases
      .getOrElse(clazz, throw new RuntimeException("model base not found for class " + clazz))
      .asInstanceOf[B]
  }

}
