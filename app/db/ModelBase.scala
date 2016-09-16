package db

/**
  * Represents something that provides access to a ModelTable.
  *
  * @tparam T ModelTable
  * @tparam M Model
  */
trait ModelBase[T <: ModelTable[M], M <: Model] {

  /** The [[Model]] that this provides access to */
  val modelClass: Class[M]
  /** The [[ModelService]] to retrieve the model */
  val service: ModelService

  /**
    * Provides access to the ModelTable.
    *
    * @return ModelAccess
    */
  def access: ModelAccess[T, M] = this.service.access[T, M](this.modelClass)

}

object ModelBase {
  implicit def unwrap[T <: ModelTable[M], M <: Model](base: ModelBase[T, M]): ModelAccess[T, M] = base.access
}
