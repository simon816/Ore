package db.impl

import db.meta.ModelProcessor
import db.{Model, ModelService, ModelTable}
import forums.DiscourseApi
import ore.project.util.ProjectFileManager
import util.OreConfig

import scala.reflect.runtime.universe._

class OreModelProcessor(service: ModelService,
                        config: OreConfig)
                        extends ModelProcessor(service, config) {

  override def process[T <: ModelTable[M], M <: Model[_]: TypeTag](model: M) = {
    super.process(model)
    model match {
      case oreModel: OreModel[_] => oreModel.config = this.config
      case _ =>
    }
  }
}
