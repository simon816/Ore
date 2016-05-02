package db.dao

import db.model.{Model, ModelTable}

/**
  * Represents the base ModelSet containing all models in the table.
  *
  * @param modelClass Model class
  * @tparam T         Table
  * @tparam M         Model
  */
class ModelSet[T <: ModelTable[M], M <: Model](override protected val modelClass: Class[M]) extends TModelSet[T, M]
