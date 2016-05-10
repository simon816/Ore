package db

import db.action.ModelAccess

/**
  * Represents something that provides access to a ModelTable.
  *
  * @tparam T ModelTable
  * @tparam M Model
  */
trait ModelAccessible[T <: ModelTable[M], M <: Model] {

  val modelClass: Class[M]
  val service: ModelService

  /**
    * Provides access to the ModelTable.
    *
    * @return ModelAccess
    */
  def access: ModelAccess[T, M] = this.service.access[T, M](this.modelClass)

  implicit def access(self: ModelAccessible[T, M]): ModelAccess[T, M] = this.access

}
