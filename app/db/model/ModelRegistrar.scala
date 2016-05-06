package db.model

import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries

/**
  * A registrar for ModelQueries. This contains all the necessary information
  * to interact with any Model in the database.
  */
class ModelRegistrar {

  private var modelQueries: Map[Class[_ <: Model], ModelQueries[_, _]] = Map.empty

  /**
    * Registers a new ModelQueries.
    *
    * @param modelQueries ModelQueries to register
    * @tparam Q Type Queries type
    * @return Registered queries
    */
  def register[Q <: ModelQueries[_, _ <: Model]](modelQueries: Q): Q = {
    this.modelQueries += modelQueries.modelClass -> modelQueries
    modelQueries
  }

  /**
    * Returns a registered ModelQueries for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam M         Model type
    * @return           ModelQueries of Model
    */
  def get[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): ModelQueries[T, M] = {
    this.modelQueries.find(_._1.isAssignableFrom(modelClass))
      .getOrElse(throw new RuntimeException("queries not found for model " + modelClass))
      ._2.asInstanceOf[ModelQueries[T, M]]
  }

  /**
    * Returns the base query for the specified Model class.
    *
    * @param modelClass Model class
    * @tparam T         Table type
    * @tparam M         Model type
    * @return           Base query for Model
    */
  def modelQuery[T <: ModelTable[M], M <: Model](modelClass: Class[_ <: M]): Query[T, M, Seq]
  = this.get(modelClass).baseQuery.asInstanceOf[Query[T, M, Seq]]

}
