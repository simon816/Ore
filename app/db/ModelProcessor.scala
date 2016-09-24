package db

import com.google.common.base.Preconditions.checkNotNull

/**
  * [[Model]]s are processed immediately after they've been retrieved from the
  * database.
  */
class ModelProcessor(service: ModelService) {

  /**
    * Processes a [[Model]].
    *
    * @param model  Model to process
    * @tparam M     Model type
    */
  def process[M <: Model](model: M): M = {
    checkNotNull(model, "tried to process null model!", "")
    model.service = this.service
    model.setProcessed(true)
    model
  }

}
