package util.forums

import play.api.Play.{configuration => config, current}
import play.api.libs.ws.WSClient

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  val Auth = config.getString("discourse.sso.url").map { ssoUrl =>
    new DiscourseSSO(ssoUrl, config.getString("discourse.sso.secret").get)
  }.orNull

  private var api: DiscourseAPI = null
  def API: DiscourseAPI = this.api

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def init(ws: WSClient) = {
    this.api = new DiscourseAPI(config.getString("discourse.baseUrl").get, ws)
  }

}
