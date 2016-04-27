package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model
import db.query.Queries
import db.query.Queries.{ModelFilter, now}
import slick.lifted.ColumnOrdered

/**
  * Represents a set of Models belonging to a Model parent.
  *
  * @param parentRef    Table column that references the parent in the child table
  * @param parent       Parent model
  * @tparam ParentTable Parent table
  * @tparam Parent      Parent model
  * @tparam ChildTable  Child table
  * @tparam Child       Child model
  */
class ModelSet[ParentTable <: ModelTable[Parent], Parent <: Model, ChildTable <: ModelTable[Child], Child <: Model]
              (childClass: Class[Child], parentRef: ChildTable => Rep[Int], parent: Parent) extends ModelDAO[Child] {

  /* Filters models in ChildTable to models with a reference of Parent */
  protected val childFilter: ModelFilter[ChildTable, Child] = ModelFilter(this.parentRef(_) === this.parent.id.get)
  /* Filters models by ID */
  protected def idFilter(id: Int): ModelFilter[ChildTable, Child] = ModelFilter(_.id === id)

  /**
    * Returns all the children of the parent model.
    *
    * @return All model children
    */
  def values: Set[Child] = now(Queries.collect(childClass, filter = childFilter)).get.toSet

  /**
    * Returns a Seq of all the children of the parent model.
    *
    * @return Seq of values
    */
  def seq: Seq[Child] = this.values.toSeq

  /**
    * Returns the amount of children the parent model has.
    *
    * @return Amount of children
    */
  def size: Int = now(Queries count (childClass, childFilter)).get

  /**
    * Returns true if this set is empty.
    *
    * @return True if empty
    */
  def isEmpty: Boolean = this.size == 0

  /**
    * Returns true if this set is not empty.
    *
    * @return True if not empty
    */
  def nonEmpty: Boolean = this.size > 0

  /**
    * Returns true if the parent model has the specified child.
    *
    * @param child  Child to look for
    * @return       True if parent has child
    */
  def contains(child: Child): Boolean = now(Queries count (childClass, childFilter +& idFilter(child.id.get))).get > 0

  /**
    * Inserts the specified child.
    *
    * @param child  Child to insert
    * @return       New child
    */
  def add(child: Child): Child = now(Queries insert child).get

  /**
    * Removes the child from the set.
    *
    * @param child  Child to remove
    * @return       True if the set changed as a result
    */
  def remove(child: Child): Boolean = if (this.contains(child)) {
    now(Queries delete child).get
    true
  } else true

  /**
    * Removes all children matching the specified filter.
    *
    * @param filter Model filter
    */
  def removeWhere(filter: ChildTable => Rep[Boolean])
  = now(Queries deleteWhere (childClass, this.childFilter && filter)).get

  /**
    * Finds the first child matching the specified filter.
    *
    * @param filter Model filter
    * @return       First child
    */
  def find(filter: ChildTable => Rep[Boolean]): Option[Child]
  = now(Queries find (childClass, this.childFilter && filter)).get

  /**
    * Returns a sorted Seq of child models.
    *
    * @param ordering Ordering of Seq
    * @param filter   Model filter
    * @param limit    Amount to take
    * @param offset   Amount to drop
    * @return         Sorted child models
    */
  def sorted(ordering: ChildTable => ColumnOrdered[_], filter: ChildTable => Rep[Boolean] = null,
             limit: Int = -1, offset: Int = -1): Seq[Child] = {
    now(Queries.collect(childClass, limit, offset, this.childFilter && filter, ordering)).get.toList
  }

  /**
    * Filters the children of the parent.
    *
    * @param filter Model filter
    * @param limit  Amount to take
    * @param offset Amount to drop
    * @return       Filtered children
    */
  def filter(filter: ChildTable => Rep[Boolean], limit: Int = -1, offset: Int = -1): Seq[Child] = {
    now(Queries.collect(childClass, limit, offset, this.childFilter && filter)).get
  }

  override def withId(id: Int): Option[Child] = now(Queries.find(childClass, childFilter +& idFilter(id))).get

}
