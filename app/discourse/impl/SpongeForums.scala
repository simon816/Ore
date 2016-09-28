package discourse.impl

import java.nio.file.Path
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import db.impl.access.ProjectBase
import ore.{OreConfig, OreEnv}
import play.api.libs.ws.WSClient
import util.CryptoUtils

import scala.concurrent.duration._

@Singleton
class SpongeForums @Inject()(env: OreEnv,
                             config: OreConfig,
                             actorSystem: ActorSystem,
                             override val ws: WSClient) extends OreDiscourseApi {

  private val conf = this.config.forums

  isEnabled = this.conf.getBoolean("api.enabled").get

  override val key: String = this.conf.getString("api.key").get
  override val admin: String = this.conf.getString("api.admin").get
  override val timeout: Duration = this.conf.getInt("api.timeout").get.millis
  override val url: String = this.conf.getString("baseUrl").get

  override val secret: String = this.conf.getString("sso.secret").get
  override val ssoUrl: String = this.conf.getString("sso.url").get

  override val categorySlug: String = this.conf.getString("embed.categorySlug").get
  override val topicTemplatePath: Path = this.env.conf.resolve("discourse/project_topic.md")
  override protected val scheduler = this.actorSystem.scheduler
  override protected val retryRate: FiniteDuration = this.conf.getInt("embed.retryRate").get.millis

  override def nonce = CryptoUtils.nonce

}
