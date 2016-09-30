import javax.inject.{Inject, Singleton}

import db.ModelService
import db.impl.access.ProjectBase
import discourse.impl.OreDiscourseApi

/**
  * Handles initialization logic for the application.
  */
trait Bootstrap {

  val modelService: ModelService
  val forums: OreDiscourseApi

  val Logger = play.api.Logger("Bootstrap")

  Logger.info("Initializing Ore...")
  val time = System.currentTimeMillis()

  this.modelService.start()

  this.forums.projects = this.modelService.getModelBase(classOf[ProjectBase])
  this.forums.start()

  Logger.info(s"Ore Initialized (${System.currentTimeMillis() - time}ms).")

}

@Singleton
class BootstrapImpl @Inject()(override val modelService: ModelService,
                              override val forums: OreDiscourseApi) extends Bootstrap
