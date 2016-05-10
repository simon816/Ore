import com.google.inject.AbstractModule
import db.ModelService
import db.impl.OreModelService
import forums.{DiscourseApi, SpongeForums}
import ore.{OreStatTracker, OreUserBase, StatTracker, UserBase}
import ore.api.{OreRestful, OreRestfulApi}
import ore.project.{OreProjectBase, ProjectBase}
import ore.project.util.{OreProjectFactory, ProjectFactory}

class Module extends AbstractModule {

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[UserBase]).to(classOf[OreUserBase])
    bind(classOf[ProjectBase]).to(classOf[OreProjectBase])

    bind(classOf[OreRestfulApi]).to(classOf[OreRestful])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])
    bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
  }

}
