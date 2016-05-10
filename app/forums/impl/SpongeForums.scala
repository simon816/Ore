package forums.impl

import javax.inject.Inject

import db.ModelService
import forums.{DiscourseApi, DiscourseEmbeddingService}
import ore.UserBase
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(val users: UserBase,
                             val config: OreConfig,
                             val env: OreEnv,
                             override val ws: WSClient,
                             val service: ModelService) extends DiscourseApi {

  lazy val conf = config.forums
  override lazy val url = conf.getString("baseUrl").get

  override lazy val embed = new DiscourseEmbeddingService(
    this, url, conf.getString("api.key").get, conf.getInt("embed.categoryId").get, ws, service, config, env
  )

}
