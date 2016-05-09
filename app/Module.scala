import com.google.inject.AbstractModule
import db.ModelRegistrar
import db.impl.{OreModelProcessor, OreModelRegistrar}
import db.meta.ModelProcessor

class Module extends AbstractModule {

  def configure() = {
    bind(classOf[ModelRegistrar]).to(classOf[OreModelRegistrar])
    bind(classOf[ModelProcessor]).to(classOf[OreModelProcessor])
  }

}
