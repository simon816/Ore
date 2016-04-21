package db.orm.dao

import db.orm.ModelTable
import db.orm.model.Model

/**
  * Represents a [[ModelSet]] that cannot be modified.
  *
  * @tparam T Table type
  * @tparam M Model type
  */
trait TraitImmutableModelSet[T <: ModelTable[M], M <: Model] extends ModelSet[T, M] {
  override def add(model: M) = throw new UnsupportedOperationException
  override def remove(model: M) = throw new UnsupportedOperationException
}
