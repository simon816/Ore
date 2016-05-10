import com.google.inject.AbstractModule
import db.ModelService
import db.impl.OreModelService
import forums._
import forums.impl.{SpongeAuth, SpongeForums}
import ore.{OreStatTracker, OreUserBase, StatTracker, UserBase}
import ore.api.{OreRestful, OreRestfulApi}
import ore.project.{OreProjectBase, ProjectBase}
import ore.project.util.{OreProjectFactory, ProjectFactory}
import play.api.Configuration
import util.OreConfig

class Module(configuration: Configuration) extends AbstractModule {

  val config = new OreConfig(configuration)

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[UserBase]).to(classOf[OreUserBase])
    bind(classOf[ProjectBase]).to(classOf[OreProjectBase])

    bind(classOf[OreRestfulApi]).to(classOf[OreRestful])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])

    if (config.forums.getBoolean("api.enabled").get)
      bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
    else
      bind(classOf[DiscourseApi]).to(classOf[DisabledDiscourseApi])

    bind(classOf[DiscourseSSO]).to(classOf[SpongeAuth])
  }

}
