import com.google.inject.AbstractModule
import db.ModelService
import db.impl.service.OreModelService
import forums._
import forums.impl.{SpongeAuth, SpongeForums}
import ore._
import ore.project.util.{OreProjectFactory, OreProjectManager, ProjectFactory, ProjectManager}
import ore.rest.{OreRestfulServer, OreRestfulApi}
import play.api.{Configuration, Environment}
import util.OreConfig

/** The Ore Module */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  val config = new OreConfig(configuration)

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[ProjectManager]).to(classOf[OreProjectManager])

    bind(classOf[OreRestfulApi]).to(classOf[OreRestfulServer])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])

    if (config.forums.getBoolean("api.enabled").get)
      bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
    else
      bind(classOf[DiscourseApi]).to(classOf[DisabledDiscourseApi])

    bind(classOf[DiscourseSSO]).to(classOf[SpongeAuth])
  }

}
