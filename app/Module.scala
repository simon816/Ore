import com.google.inject.AbstractModule
import db.ModelService
import db.impl.service.OreModelService
import discourse.{OreDiscourseApi, SpongeForums}
import ore._
import ore.project.factory.{OreProjectFactory, ProjectFactory}
import ore.rest.{OreRestfulApi, OreRestfulServer}
import play.api.{Configuration, Environment}
import security.sso.{SingleSignOn, SpongeSingleSignOn}

/** The Ore Module */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure() = {
    bind(classOf[OreRestfulApi]).to(classOf[OreRestfulServer])
    bind(classOf[StatTracker]).to(classOf[OreStatTracker])
    bind(classOf[ProjectFactory]).to(classOf[OreProjectFactory])
    bind(classOf[OreDiscourseApi]).to(classOf[SpongeForums])
    bind(classOf[SingleSignOn]).to(classOf[SpongeSingleSignOn])
    bind(classOf[ModelService]).to(classOf[OreModelService])
    bind(classOf[Bootstrap]).to(classOf[BootstrapImpl]).asEagerSingleton()
  }

}
