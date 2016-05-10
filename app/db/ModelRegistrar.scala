package db

import com.google.common.collect.{BiMap, HashBiMap}
import db.action.ModelActions
import db.meta.TypeSetter

import scala.collection.JavaConverters._

/**
  * A registrar for ModelActions. This contains all the necessary information
  * to interact with any Model in the database.
  */
trait ModelRegistrar {

  private val modelActions: BiMap[Class[_ <: Model], ModelActions[_, _]] = HashBiMap.create()
  private val typeSetters: BiMap[Class[_], TypeSetter[_]] = HashBiMap.create()

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

}
