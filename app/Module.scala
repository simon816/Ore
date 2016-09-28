import com.google.inject.AbstractModule
import db.ModelService
import db.impl.service.OreModelService
import discourse.impl.{OreDiscourseApi, SpongeForums}
import ore.project.factory.{OreProjectFactory, ProjectFactory}
import ore.rest.{OreRestfulApi, OreRestfulServer}
import ore.{OreConfig, _}
import play.api.{Configuration, Environment}

/** The Ore Module */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  val config = new OreConfig(configuration)

  def configure() = {
    bind(classOf[OreRestfulApi]).to(classOf[OreRestfulServer])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[OreDiscourseApi]).to(classOf[SpongeForums])
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[OreBootstrap]).to(classOf[OreBootstrapImpl]).asEagerSingleton()
  }

}
