import javax.inject.{Inject, Singleton}

import db.ModelService
import db.impl.access.ProjectBase
import discourse.impl.OreDiscourseApi

trait OreBootstrap {

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
class OreBootstrapImpl @Inject()(override val modelService: ModelService,
                                 override val forums: OreDiscourseApi) extends OreBootstrap
