package db

/**
  * An immutable version of [[ModelAccess]].
  *
  * @param service    ModelService instance
  * @param modelClass Model class
  * @param baseFilter Base filter
  * @tparam T         Table type
  * @tparam M         Model type
  */
case class ImmutableModelAccess[T <: ModelTable[M], M <: Model]
(override val service: ModelService,
 override val modelClass: Class[M],
 override val baseFilter: ModelFilter[T, M] = ModelFilter[T, M]())
  extends ModelAccess[T, M](service, modelClass, baseFilter) {

  def this(mutable: ModelAccess[T, M]) = this(mutable.service, mutable.modelClass, mutable.baseFilter)

  override def add(model: M) = throw new UnsupportedOperationException
  override def remove(model: M) = throw new UnsupportedOperationException
  override def removeAll(filter: Filter) = throw new UnsupportedOperationException

}

object ImmutableModelAccess {

  def apply[T <: ModelTable[M], M <: Model](mutable: ModelAccess[T, M]): ImmutableModelAccess[T, M]
  = new ImmutableModelAccess(mutable)

}
