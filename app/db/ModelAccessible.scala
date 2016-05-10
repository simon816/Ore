package db

import db.action.ModelAccess

trait ModelAccessible[T <: ModelTable[M], M <: Model] {

  val modelClass: Class[M]
  val service: ModelService

  implicit def access(self: ModelAccessible): ModelAccess[T, M] = service.access[T, M](modelClass)

}
