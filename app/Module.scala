import com.google.inject.AbstractModule
import db.impl.{OreModelProcessor, OreModelRegistrar, OreModelService}
import db.meta.ModelProcessor
import db.{ModelRegistrar, ModelService}
import forums.{DiscourseApi, SpongeForums}
import ore.api.{OreRestful, OreRestfulApi}

class Module extends AbstractModule {

  def configure() = {
    // Models
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ModelRegistrar]).to(classOf[OreModelRegistrar])
    bind(classOf[ModelProcessor]).to(classOf[OreModelProcessor])

    // Discourse
    bind(classOf[DiscourseApi]).to(classOf[SpongeForums])

    // RESTful API
    bind(classOf[OreRestfulApi]).to(classOf[OreRestful])
  }

}
