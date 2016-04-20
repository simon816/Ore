package db.orm.dao

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
  * @param parentRef  Column that references the models contained in this set
  * @tparam T         Table type
  * @tparam M         Model type
  */
class ModelSet[T <: ModelTable[M], M <: Model](queries: Queries[T, M],
                                               parentId: Int,
                                               parentRef: T => Rep[Int])
                                               extends ModelDAO[M] {

  /**
    * Returns all models in this set.
    *
    * @return All models in set
    */
  def values: Set[M] = now(this.queries collect (filter = parentRef(_) === parentId)).get.toSet

  /**
    * Returns the amount of models in this set.
    *
    * @return Amount of models in set
    */
  def size: Int = now(this.queries count (parentRef(_) === parentId)).get

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
    * Returns true if this set contains the given model.
    *
    * @param model  Model to look for
    * @return       True if set contains model
    */
  def contains(model: M): Boolean = {
    now(this.queries count (t => parentRef(t) === parentId && t.id === model.id.get)).get > 0
  }

  /**
    * Adds a new model to the set.
    *
    * @param model  Model to add
    * @return       The newly created model
    */
  def add(model: M): M = now(this.queries create model).get

  /**
    * Removes a model from the set.
    *
    * @param model  Model to remove
    * @return       True if the model was removed
    */
  def remove(model: M): Boolean = if (this.contains(model)) {
    now(this.queries delete model).get
    true
  } else {
    false
  }

  /**
    * Removes all models matching the specified filter.
    *
    * @param p Model filter
    */
  def removeWhere(p: T => Rep[Boolean]) = now(this.queries deleteWhere p).get

  /**
    * Finds the first model that matches the given predicate.
    *
    * @param p  Predicate filter
    * @return   First match
    */
  def find(p: T => Rep[Boolean]): Option[M] = now(this.queries ? (m => parentRef(m) === parentId && p(m))).get

  /**
    * Filters the values in this set by the given predicate.
    *
    * @param p  Predicate filter
    * @return   Result sequence
    */
  def filter(p: T => Rep[Boolean]): Seq[M] = now(this.queries.collect(filter = m => parentRef(m) === parentId && p(m))).get

  /**
    * Returns a Seq version of this set.
    *
    * @return Sequence
    */
  def seq: Seq[M] = this.values.toSeq

  override def withId(id: Int): Option[M] = now(this.queries.get(id)).get

}
