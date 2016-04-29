package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model
import db.query.ModelQueries
import db.query.ModelQueries._
import slick.lifted.ColumnOrdered

/**
  * A basic Model data access object.
  */
trait TModelSet[T <: ModelTable[M], M <: Model] {

  /** The model class */
  protected def modelClass: Class[M]
  /** The base filter to use */
  protected def baseFilter: ModelFilter[T, M] = ModelFilter()

  /**
    * Returns the model with the specified ID.
    *
    * @param id   ID to lookup
    * @return     Model with ID or None if not found
    */
  def withId(id: Int): Option[M] = await(get(this.modelClass, id, this.baseFilter)).get

  /**
    * Returns all the [[Model]]s in the set.
    *
    * @return All models in set
    */
  def values: Set[M] = await(ModelQueries.filter(this.modelClass, this.baseFilter)).get.toSet

  /**
    * Returns the size of this set.
    *
    * @return Size of set
    */
  def size: Int = await(count (this.modelClass, this.baseFilter)).get

  /**
    * Returns true if this set is empty.
    *
    * @return True if set is empty
    */
  def isEmpty: Boolean = this.size == 0

  /**
    * Returns true if this set is not empty.
    *
    * @return True if not empty
    */
  def nonEmpty: Boolean = this.size > 0

  /**
    * Returns true if this set contains the specified model.
    *
    * @param model Model to look for
    * @return True if contained in set
    */
  def contains(model: M): Boolean = await(count (this.modelClass, this.baseFilter +&& IdFilter(model.id.get))).get > 0

  /**
    * Adds a new model to it's table.
    *
    * @param model Model to add
    * @return New model
    */
  def add(model: M): M = await(insert(model)).get

  /**
    * Removes the specified model from this set if it is contained.
    *
    * @param model Model to remove
    */
  def remove(model: M) = await(delete(model, this.baseFilter)).get

  /**
    * Removes all the models from this set matching the given filter.
    *
    * @param filter Filter to use
    */
  def removeAll(filter: T => Rep[Boolean]) = await(deleteWhere(modelClass, this.baseFilter && filter))

  /**
    * Returns the first model matching the specified filter.
    *
    * @param filter Filter to use
    * @return       Model matching filter, if any
    */
  def find(filter: T => Rep[Boolean]): Option[M]
  = await(ModelQueries.find(this.modelClass, this.baseFilter && filter)).get

  /**
    * Returns a sorted Seq by the specified [[ColumnOrdered]].
    *
    * @param ordering Model ordering
    * @param filter   Filter to use
    * @param limit    Amount to take
    * @param offset   Amount to drop
    * @return         Sorted models
    */
  def sorted(ordering: T => ColumnOrdered[_], filter: T => Rep[Boolean] = null,
             limit: Int = -1, offset: Int = -1): Seq[M] = {
    await(collect(this.modelClass, this.baseFilter && filter, ordering, limit, offset)).get
  }

  /**
    * Filters this set by the given function.
    *
    * @param filter Filter to use
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       Filtered models
    */
  def filter(filter: T => Rep[Boolean], limit: Int = -1, offset: Int = -1): Seq[M]
  = await(ModelQueries.filter(this.modelClass, filter, limit, offset)).get

  /**
    * Returns a Seq of this set.
    *
    * @return Seq of set
    */
  def toSeq: Seq[M] = this.values.toSeq

}
