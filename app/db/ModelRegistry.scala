package db

import com.google.common.collect.{BiMap, HashBiMap}
import db.action.ModelActions
import db.meta.TypeSetter

import scala.collection.JavaConverters._

/**
  * A registrar for ModelActions. This contains all the necessary information
  * to interact with any Model in the database.
  */
trait ModelRegistry {

  private val modelActions: BiMap[Class[_ <: Model], ModelActions[_, _]] = HashBiMap.create()
  private val typeSetters: BiMap[Class[_], TypeSetter[_]] = HashBiMap.create()
  private var modelBases: Map[Class[_ <: ModelBase[_, _]], ModelBase[_, _]] = Map.empty

  /**
    * Registers a new ModelActions.
    *
    * @param actions ModelActions to register
    * @tparam Q Type Actions type
    * @return Registered actions
    */
  def register[Q <: ModelActions[_, _ <: Model]](actions: Q): Q = {
    this.modelActions.put(actions.modelClass, actions)
    actions
  }

  /**
    * Registers a new TypeSetter that can be bound to model fields.
    *
    * @param clazz  Type class
    * @param setter Type setter
    * @tparam A     Type
    */
  def registerSetter[A](clazz: Class[A], setter: TypeSetter[A]): TypeSetter[A] = {
    typeSetters.put(clazz, setter)
    setter
  }

  /**
    * Returns a registered TypeSetter by the type class.
    *
    * @param clazz  Type class
    * @tparam A     Type
    * @return       Registered setter, if any, None otherwise
    */
  def getSetter[A](clazz: Class[A]): TypeSetter[A] = {
    typeSetters.asScala.get(clazz)
      .map(_.asInstanceOf[TypeSetter[A]])
      .getOrElse(throw new RuntimeException("No type setter found for type: " + clazz.getSimpleName))
  }

  /**
    * Finds a ModelActions instance by the ModelActions class.
    *
    * @param actionsClass ModelActions class
    * @tparam Q           Actions type
    * @return             ModelActions
    */
  //noinspection ComparingUnrelatedTypes
  def getActions[Q <: ModelActions[_, _]](actionsClass: Class[Q]): Q
  = this.modelActions.asScala.find(_._2.getClass.equals(actionsClass))
    .getOrElse(throw new RuntimeException("actions not found of type " + actionsClass))
    ._2.asInstanceOf[Q]

  /**
    * Returns a registered ModelActions for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam M         Model type
    * @return           ModelActions of Model
    */
  def getActionsByModel[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): ModelActions[T, M] = {
    this.modelActions.asScala.find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("actions not found for model " + modelClass))
      ._2.asInstanceOf[ModelActions[T, M]]
  }

  /**
    * Registers a new [[ModelBase]] with the service.
    *
    * @param clazz  ModelBase class
    * @param base   ModelBase
    * @tparam B     Type
    */
  def registerModelBase[B <: ModelBase[_, _]](clazz: Class[B], base: B): B = {
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
  def getModelBase[B <: ModelBase[_, _]](clazz: Class[B]): B = {
    this.modelBases
      .getOrElse(clazz, throw new RuntimeException("model base not found for class " + clazz))
      .asInstanceOf[B]
  }

}
