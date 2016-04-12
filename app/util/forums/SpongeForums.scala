package util.forums

import play.api.Play.{configuration => config, current}
import play.api.libs.ws.WSClient

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  val Auth = new DiscourseSSO(config.getString("discourse.sso.url").get, config.getString("discourse.sso.secret").get)
  private var groups: DiscourseGroups = null
  def Groups: DiscourseGroups = this.groups

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def init(ws: WSClient) = {
    this.groups = new DiscourseGroups(config.getString("discourse.baseUrl").get, ws)
  }

}
