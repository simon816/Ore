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
    * @param self This
    * @return     ModelAccess
    */
  implicit def access(self: ModelAccessible): ModelAccess[T, M] = service.access[T, M](modelClass)

}
