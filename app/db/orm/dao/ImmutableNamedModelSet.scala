package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.NamedModelTable
import db.orm.model.NamedModel
import db.query.Queries

/**
  * Represents a [[NamedModelSet]] that cannot be modified.
  *
  * @param queries    Queries class
  * @param parentId   Parent model ID
  * @param parentRef  Column that references the models contained in this set
  * @tparam T         Table type
  * @tparam M         Model type
  */
class ImmutableNamedModelSet[T <: NamedModelTable[M],
                             M <: NamedModel](queries: Queries[T, M],
                                              parentId: Int,
                                              parentRef: T => Rep[Int])
                                              extends NamedModelSet[T, M](queries, parentId, parentRef)
                                              with TImmutableModelSet[T, M]
