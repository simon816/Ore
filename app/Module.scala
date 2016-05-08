import com.google.inject.AbstractModule
import db.ModelRegistrar
import db.impl.OreModelRegistrar
import db.meta.{BindingsGenerator, OreBindingsGenerator}

class Module extends AbstractModule {

  def configure() = {
    bind(classOf[ModelRegistrar]).to(classOf[OreModelRegistrar])
    bind(classOf[BindingsGenerator]).to(classOf[OreBindingsGenerator])
  }

}
