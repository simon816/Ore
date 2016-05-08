package db

import javax.inject.Singleton

import com.google.common.collect.{BiMap, HashBiMap}
import db.meta.TypeSetters.TypeSetter
import db.action.ModelActions

import scala.collection.JavaConverters._

/**
  * A registrar for ModelQueries. This contains all the necessary information
  * to interact with any Model in the database.
  */
trait ModelRegistrar {

  private val modelActions: BiMap[Class[_ <: Model[_]], ModelActions[_, _]] = HashBiMap.create()
  private val typeSetters: BiMap[Class[_], TypeSetter[_]] = HashBiMap.create()

  /**
    * Registers a new ModelQueries.
    *
    * @param modelQueries ModelQueries to register
    * @tparam Q Type Queries type
    * @return Registered queries
    */
  def register[Q <: ModelActions[_, _ <: Model[_]]](modelQueries: Q): Q = {
    this.modelActions.put(modelQueries.modelClass, modelQueries)
    modelQueries
  }

  def registerSetter[A](clazz: Class[A], setter: TypeSetter[A]) = typeSetters.put(clazz, setter)

  def getSetter[A](clazz: Class[A]): Option[TypeSetter[A]]
  = typeSetters.asScala.get(clazz).map(_.asInstanceOf[TypeSetter[A]])

  def reverseLookup[Q <: ModelActions[_, _]](actionsClass: Class[Q]): Q
  = this.modelActions.asScala.find(_._2.getClass.equals(actionsClass)).get._2.asInstanceOf[Q]

  /**
    * Returns a registered ModelQueries for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam M         Model type
    * @return           ModelQueries of Model
    */
  def get[T <: ModelTable[M], M <: Model[_]](modelClass: Class[_ <: M]): ModelActions[T, M] = {
    this.modelActions.asScala.find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("queries not found for model " + modelClass))
      ._2.asInstanceOf[ModelActions[T, M]]
  }

}
