package util.forums

import play.api.Play.{configuration => config, current}
import play.api.libs.ws.WSClient

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  lazy val Auth = new DiscourseSSO(config.getString("discourse.sso.url").get,
                                   config.getString("discourse.sso.secret").get)

  private var api: DiscourseAPI = null
  def API: DiscourseAPI = this.api

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def init(ws: WSClient) = {
    if (config.getBoolean("discourse.api.enabled").get) {
      this.api = new DiscourseAPI(config.getString("discourse.baseUrl").get, ws)
    } else {
      this.api = DiscourseAPI.Disabled
    }
  }

}
