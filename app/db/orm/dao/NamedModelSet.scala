package db.orm.dao

import db.OrePostgresDriver.api._
import db.orm.NamedModelTable
import db.orm.model.NamedModel
import db.query.Queries
import db.query.Queries._
import slick.lifted.Rep

class NamedModelSet[T <: NamedModelTable[M], M <: NamedModel](queries: Queries[T, M],
                                                              parentId: Int,
                                                              ref: T => Rep[Int])
                                                              extends ModelSet[T, M](queries, parentId, ref) {

  /**
    * Returns the model with the specified name.
    *
    * @param name   Model name
    * @return       Model with name or None if not found
    */
  def withName(name: String): Option[M] = {
    now(this.queries ? (m => ref(m) === parentId && m.modelName === name)).get
  }

}
