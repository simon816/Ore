import java.security.Security
import javax.inject.{Inject, Singleton}

import db.ModelService
import db.impl.access.ProjectBase
import discourse.OreDiscourseApi
import ore.OreConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
  * Handles initialization logic for the application.
  */
trait Bootstrap {

  val modelService: ModelService
  val forums: OreDiscourseApi
  val config: OreConfig

  val Logger = play.api.Logger("Bootstrap")

  Logger.info("Initializing Ore...")
  val time = System.currentTimeMillis()

  this.modelService.start()

  this.forums.projects = this.modelService.getModelBase(classOf[ProjectBase])
  this.forums.start()

  if (this.config.security.getBoolean("requirePgp").get)
    Security.addProvider(new BouncyCastleProvider)

  Logger.info(s"Ore Initialized (${System.currentTimeMillis() - time}ms).")

}

@Singleton
class BootstrapImpl @Inject()(override val modelService: ModelService,
                              override val forums: OreDiscourseApi,
                              override val config: OreConfig) extends Bootstrap
