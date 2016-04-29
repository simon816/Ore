package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model

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
class ChildModelSet[ParentTable <: ModelTable[Parent], Parent <: Model,
                    ChildTable <: ModelTable[Child], Child <: Model]
                   (override protected val modelClass: Class[Child],
                    parentRef: ChildTable => Rep[Int],
                    parent: Parent) extends TModelSet[ChildTable, Child] {
  /* Filters models in ChildTable to models with a reference of Parent */
  override protected val baseFilter: ModelFilter[ChildTable, Child] = ModelFilter(this.parentRef(_) === this.parent.id.get)
}
