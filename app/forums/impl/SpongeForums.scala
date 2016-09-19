package forums.impl

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.ActorSystem
import forums.{DiscourseApi, DiscourseEmbeddingService, DiscourseSync}
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

import scala.concurrent.duration.Duration

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(env: OreEnv,
                             config: OreConfig,
                             actorSystem: ActorSystem,
                             override val ws: WSClient) extends DiscourseApi {

  lazy val conf = this.config.forums

  override lazy val url = this.conf.getString("baseUrl").get
  override lazy val key = this.conf.getString("api.key").get
  override lazy val admin = this.conf.getString("api.admin").get
  override lazy val sync = new DiscourseSync(
    this.actorSystem.scheduler, Duration(this.config.forums.getInt("embed.retryRate").get, TimeUnit.MILLISECONDS)
  )

  override lazy val embed = if (!this.conf.getBoolean("embed.disabled").get) {
    new DiscourseEmbeddingService(
      api = this,
      url = this.url,
      key = this.key,
      categoryId = this.conf.getInt("embed.categoryId").get,
      ws = ws,
      config = config,
      env = env
    )
  } else DiscourseEmbeddingService.Disabled

}
