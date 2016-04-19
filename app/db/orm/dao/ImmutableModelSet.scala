package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.ModelTable
import db.orm.model.Model
import db.query.Queries

class ImmutableModelSet[T <: ModelTable[M], M <: Model](queries: Queries[T, M],
                                                        parentId: Int,
                                                        parentRef: T => Rep[Int])
                                                        extends ModelSet[T, M](queries, parentId, parentRef)
                                                        with TraitImmutableModelSet[T, M]
