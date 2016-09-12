package forums.impl

import javax.inject.Inject

import akka.actor.ActorSystem
import forums.{DiscourseApi, DiscourseEmbeddingService}
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(config: OreConfig,
                             env: OreEnv,
                             actorSystem: ActorSystem,
                             override val ws: WSClient) extends DiscourseApi {

  lazy val conf = this.config.forums

  override lazy val url = this.conf.getString("baseUrl").get
  override lazy val key = this.conf.getString("api.key").get
  override lazy val admin = this.conf.getString("api.admin").get

  override lazy val embed = if (!this.conf.getBoolean("embed.disabled").get) {
    new DiscourseEmbeddingService(
      api = this,
      url = this.url,
      key = this.key,
      categoryId = this.conf.getInt("embed.categoryId").get,
      ws = ws,
      config = config,
      env = env,
      actorSystem = actorSystem
    )
  } else DiscourseEmbeddingService.Disabled

}
