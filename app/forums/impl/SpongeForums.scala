package forums.impl

import javax.inject.Inject

import forums.{DiscourseApi, DiscourseEmbeddingService}
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(val config: OreConfig,
                             val env: OreEnv,
                             override val ws: WSClient) extends DiscourseApi {

  lazy val conf = config.forums
  override lazy val url = conf.getString("baseUrl").get

  override lazy val embed = new DiscourseEmbeddingService(
    this, url, conf.getString("api.key").get, conf.getInt("embed.categoryId").get, ws, config, env
  )

}
