package discourse.impl

import javax.inject.Inject

import discourse.DiscourseApi
import discourse.DiscourseSSO
import ore.OreConfig
import play.api.libs.ws.WSClient
import util.CryptoUtils

import scala.concurrent.duration._

class SpongeForums @Inject()(config: OreConfig, override val ws: WSClient) extends DiscourseApi with DiscourseSSO {

  private val conf = this.config.forums

  override var isEnabled: Boolean = this.conf.getBoolean("api.enabled").get

  override val key: String = this.conf.getString("api.key").get
  override val admin: String = this.conf.getString("api.admin").get
  override val timeout: Duration = this.conf.getInt("api.timeout").get.millis
  override val url: String = this.conf.getString("baseUrl").get
  override val secret: String = this.conf.getString("sso.secret").get
  override val ssoUrl: String = this.conf.getString("sso.url").get

  override def nonce = CryptoUtils.nonce

}
