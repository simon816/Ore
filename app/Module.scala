import com.google.inject.AbstractModule
import db.ModelService
import db.impl.OreModelService
import discourse._
import discourse.impl.SpongeForums
import ore.project.factory.{OreProjectFactory, ProjectFactory}
import ore.rest.{OreRestfulApi, OreRestfulServer}
import ore.{OreConfig, _}
import play.api.{Configuration, Environment}

/** The Ore Module */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  val config = new OreConfig(configuration)

  def configure() = {
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])

    bind(classOf[OreRestfulApi]).to(classOf[OreRestfulServer])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])

    bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
  }

}
