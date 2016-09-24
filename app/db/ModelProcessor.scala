package db

/**
  * Processes an annotated Model and bind's the appropriate fields and
  * children.
  */
class ModelProcessor(service: ModelService) {

  /**
    * Processes and generates annotation bindings for the specified model.
    *
    * @param model  Model to bind
    * @tparam M     Model type
    */
  def process[M <: Model](model: M): M = {
    model.service = this.service
    model.setProcessed(true)
    model
  }

}
