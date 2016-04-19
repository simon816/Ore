package db.orm.dao

import db.orm.ModelTable
import db.orm.model.Model

trait TraitImmutableModelSet[T <: ModelTable[M], M <: Model] extends ModelSet[T, M] {
  override def add(model: M) = throw new UnsupportedOperationException
  override def remove(model: M) = throw new UnsupportedOperationException
}
