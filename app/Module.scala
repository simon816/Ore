import com.google.inject.AbstractModule
import db.impl.{OreModelProcessor, OreModelRegistrar, OreModelService}
import db.meta.ModelProcessor
import db.{ModelRegistrar, ModelService}
import forums.{DiscourseApi, SpongeForums}
import ore.api.{OreRestful, OreRestfulApi}
import ore.project.util.{OreProjectFactory, ProjectFactory}
import ore.statistic.{OreStatTracker, StatTracker}

class Module extends AbstractModule {

  def configure() = {
    // Models
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ModelRegistrar]).to(classOf[OreModelRegistrar])
    bind(classOf[ModelProcessor]).to(classOf[OreModelProcessor])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])

    // Discourse
    bind(classOf[DiscourseApi]).to(classOf[SpongeForums])

    // Other Ore services
    bind(classOf[OreRestfulApi]).to(classOf[OreRestful])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])
  }

}
