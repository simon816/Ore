package modules

import com.google.inject.AbstractModule
import db.impl.{OreModelProcessor, OreModelRegistrar, OreModelService}
import db.meta.ModelProcessor
import db.{ModelRegistrar, ModelService}

class ModelModule extends AbstractModule {

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ModelRegistrar]).to(classOf[OreModelRegistrar])
    bind(classOf[ModelProcessor]).to(classOf[OreModelProcessor])
  }

}
