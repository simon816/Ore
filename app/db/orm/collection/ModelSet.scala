package db.orm.collection

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model
import db.query.Queries
import db.query.Queries.now

/**
  * Represents a collection of models belonging to a parent model.
  *
  * @param queries    Queries class
  * @param parentId   Parent model ID
  * @param ref        Column that references the models contained in this set
  * @tparam T         Table type
  * @tparam M         Model type
  */
class ModelSet[T <: ModelTable[M], M <: Model](queries: Queries[T, M], parentId: Int, ref: T => Rep[Int]) {

  /**
    * Returns all models in this set.
    *
    * @return All models in set
    */
  def values: Set[M] = now(this.queries ? (ref(_) === parentId)).get.toSet

  /**
    * Returns the amount of models in this set.
    *
    * @return Amount of models in set
    */
  def size: Int = now(this.queries count (ref(_) === parentId)).get

  /**
    * Returns true if there are no models in this set.
    *
    * @return True if empty
    */
  def isEmpty: Boolean = this.size == 0

  /**
    * Returns true if there are any models in this set.
    *
    * @return True if not empty
    */
  def nonEmpty: Boolean = this.size > 0

  /**
    * Adds a new model to the set.
    *
    * @param model Model to add
    */
  def add(model: M) = now(this.queries create model).get

  /**
    * Removes a model from the set.
    *
    * @param model Model to remove
    */
  def remove(model: M) = now(this.queries delete model).get

  /**
    * Returns the model with the specified ID.
    *
    * @param id   ID to lookup
    * @return     Model with ID or None if not found
    */
  def withId(id: Int): Option[M] = now(this.queries.get(id)).get

  /**
    * Finds the first model that matches the given predicate.
    *
    * @param p  Predicate filter
    * @return   First match
    */
  def find(p: T => Rep[Boolean]): Option[M] = now(this.queries ? (m => ref(m) === parentId && p(m))).get

  /**
    * Filters the values in this set by the given predicate.
    *
    * @param p  Predicate filter
    * @return   Result sequence
    */
  def filter(p: T => Rep[Boolean]): Seq[M] = now(this.queries.collect(filter = m => ref(m) === parentId && p(m))).get

  /**
    * Returns a Seq version of this set.
    *
    * @return Sequence
    */
  def seq: Seq[M] = this.values.toSeq

}
