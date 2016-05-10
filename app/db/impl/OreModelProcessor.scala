package db.impl

import db.meta.ModelProcessor
import db.{Model, ModelService, ModelTable}
import forums.DiscourseApi
import ore.project.util.ProjectFileManager
import util.OreConfig

import scala.reflect.runtime.universe._

/**
  * A ModelProcessor that performs Ore specific model processing.
  *
  * @param service  ModelService
  * @param config   Ore config
  */
class OreModelProcessor(service: ModelService,
                        config: OreConfig)
                        extends ModelProcessor(service) {

  override def process[T <: ModelTable[M], M <: Model: TypeTag](model: M) = {
    super.process(model)
    model match {
      case oreModel: OreModel => oreModel.config = this.config
      case _ =>
    }
  }
}
