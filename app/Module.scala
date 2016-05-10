import com.google.inject.AbstractModule
import db.ModelService
import db.impl.OreModelService
import forums.{DiscourseApi, SpongeForums}
import ore.api.{OreRestful, OreRestfulApi}
import ore.project.util.{OreProjectFactory, ProjectFactory}
import ore.statistic.{OreStatTracker, StatTracker}

class Module extends AbstractModule {

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[OreRestfulApi]).to(classOf[OreRestful])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])
  }

}
