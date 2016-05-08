package db.query

import db.impl.OrePostgresDriver.api._
import db.query.ModelFilter.{IdFilter, unwrapFilter}
import db.{Model, ModelService, ModelTable}
import slick.lifted.ColumnOrdered

/**
  * A basic Model data access object.
  */
class ModelSet[T <: ModelTable[M], M <: Model[_]](modelClass: Class[M],
                                                  baseFilter: ModelFilter[T, M] = ModelFilter[T, M]()) {

  def baseQuery(implicit service: ModelService): ModelQueries[T, M] = service.registrar.get[T, M](modelClass)

  /**
    * Returns the model with the specified ID.
    *
    * @param id   ID to lookup
    * @return     Model with ID or None if not found
    */
  def withId(id: Int)(implicit service: ModelService): Option[M]
  = service.await(baseQuery.get(id, this.baseFilter.fn)).get

  /**
    * Returns all the [[Model]]s in the set.
    *
    * @return All models in set
    */
  def values(implicit service: ModelService): Set[M] = service.await(baseQuery.filter(this.baseFilter)).get.toSet

  /**
    * Returns the size of this set.
    *
    * @return Size of set
    */
  def size(implicit service: ModelService): Int = service.await(baseQuery count this.baseFilter).get

  /**
    * Returns true if this set is empty.
    *
    * @return True if set is empty
    */
  def isEmpty(implicit service: ModelService): Boolean = this.size == 0

  /**
    * Returns true if this set is not empty.
    *
    * @return True if not empty
    */
  def nonEmpty(implicit service: ModelService): Boolean = this.size > 0

  /**
    * Returns true if this set contains the specified model.
    *
    * @param model Model to look for
    * @return True if contained in set
    */
  def contains(model: M)(implicit service: ModelService): Boolean
  = service.await(baseQuery count (this.baseFilter +&& IdFilter(model.id.get))).get > 0

  /**
    * Returns true if any models match the specified filter.
    *
    * @param filter Filter to use
    * @return       True if any model matches
    */
  def exists(filter: T => Rep[Boolean])(implicit service: ModelService)
  = service.await(baseQuery count (this.baseFilter && filter)).get > 0

  /**
    * Adds a new model to it's table.
    *
    * @param model Model to add
    * @return New model
    */
  def add(model: M)(implicit service: ModelService): M = service.await(baseQuery insert model).get

  /**
    * Removes the specified model from this set if it is contained.
    *
    * @param model Model to remove
    */
  def remove(model: M)(implicit service: ModelService) = service.await(baseQuery delete model).get

  /**
    * Removes all the models from this set matching the given filter.
    *
    * @param filter Filter to use
    */
  def removeAll(filter: T => Rep[Boolean])(implicit service: ModelService)
  = service.await(baseQuery deleteWhere (this.baseFilter && filter))

  /**
    * Returns the first model matching the specified filter.
    *
    * @param filter Filter to use
    * @return       Model matching filter, if any
    */
  def find(filter: T => Rep[Boolean])(implicit service: ModelService): Option[M]
  = service.await(baseQuery.find(this.baseFilter && filter)).get

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
             limit: Int = -1, offset: Int = -1)(implicit service: ModelService): Seq[M]
  = service.await(baseQuery.collect(limit, offset, this.baseFilter && filter, ordering)).get

  /**
    * Filters this set by the given function.
    *
    * @param filter Filter to use
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       Filtered models
    */
  def filter(filter: T => Rep[Boolean], limit: Int = -1, offset: Int = -1)(implicit service: ModelService): Seq[M]
  = service.await(baseQuery.filter(filter, limit, offset)).get

  /**
    * Filters this set by the opposite of the given function.
    *
    * @param filter Filter to use
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       Filtered models
    */
  def filterNot(filter: T => Rep[Boolean], limit: Int = -1, offset: Int = -1)(implicit service: ModelService): Seq[M]
  = this.filter(!filter(_), limit, offset)

  /**
    * Returns a Seq of this set.
    *
    * @return Seq of set
    */
  def toSeq(implicit service: ModelService): Seq[M] = this.values.toSeq

}
