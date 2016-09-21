import com.google.inject.AbstractModule
import db.ModelService
import db.impl.OreModelService
import forums._
import forums.impl.{SpongeAuth, SpongeForums}
import ore.{OreConfig, _}
import ore.project.factory.{OreProjectFactory, ProjectFactory}
import ore.rest.{OreRestfulApi, OreRestfulServer}
import play.api.{Configuration, Environment}

/** The Ore Module */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  val config = new OreConfig(configuration)

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])

    bind(classOf[OreRestfulApi]).to(classOf[OreRestfulServer])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])

    if (this.config.forums.getBoolean("api.enabled").get)
      bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
    else
      bind(classOf[DiscourseApi]).to(classOf[DisabledDiscourseApi])

    bind(classOf[DiscourseSSO]).to(classOf[SpongeAuth])
  }

}
