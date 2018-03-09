package discourse

import java.nio.file.Path
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import ore.{OreConfig, OreEnv}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

/**
  * [[OreDiscourseApi]] implementation.
  */
@Singleton
class SpongeForums @Inject()(env: OreEnv,
                             config: OreConfig,
                             actorSystem: ActorSystem,
                             override val ws: WSClient) extends OreDiscourseApi {

  private val conf = this.config.forums

  isEnabled = this.conf.getBoolean("api.enabled").get
  isDebugMode = this.config.ore.getBoolean("debug").getOrElse(false)

  override val key: String = this.conf.getString("api.key").get
  override val admin: String = this.conf.getString("api.admin").get
  override val timeout: Duration = this.conf.getInt("api.timeout").get.millis
  override val url: String = this.conf.getString("baseUrl").get
  override val baseUrl: String = this.config.app.getString("baseUrl").get

  override val categorySlug: String = this.conf.getString("embed.categorySlug").get
  override val topicTemplatePath: Path = this.env.conf.resolve("discourse/project_topic.md")
  override val versionReleasePostTemplatePath: Path = this.env.conf.resolve("discourse/version_post.md")
  override val scheduler = this.actorSystem.scheduler
  override val retryRate: FiniteDuration = this.conf.getInt("embed.retryRate").get.millis

}
