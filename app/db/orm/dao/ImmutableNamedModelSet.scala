package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.NamedModelTable
import db.orm.model.NamedModel
import db.query.Queries

class ImmutableNamedModelSet[T <: NamedModelTable[M],
                             M <: NamedModel](queries: Queries[T, M],
                                              parentId: Int,
                                              parentRef: T => Rep[Int])
                                              extends NamedModelSet[T, M](queries, parentId, parentRef)
                                              with TraitImmutableModelSet[T, M]
