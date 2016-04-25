package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model
import db.query.Queries

/**
  * Represents a [[ModelSet]] that cannot be modified.
  *
  * @param queries    Queries class
  * @param parentId   Parent model ID
  * @param parentRef  Column that references the models contained in this set
  * @tparam T         Table type
  * @tparam M         Model type
  */
class ImmutableModelSet[T <: ModelTable[M], M <: Model](queries: Queries[T, M],
                                                        parentId: Int,
                                                        parentRef: T => Rep[Int])
                                                        extends ModelSet[T, M](queries, parentId, parentRef)
                                                        with TImmutableModelSet[T, M]
