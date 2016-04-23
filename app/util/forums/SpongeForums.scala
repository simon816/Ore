package util.forums

import play.api.Play.{configuration => config, current}
import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSResponse}

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  lazy val Auth = new DiscourseSSO(config.getString("discourse.sso.url").get,
                                   config.getString("discourse.sso.secret").get)

  private var users: DiscourseUsers = null
  def Users: DiscourseUsers = this.users

  private var embed: DiscourseEmbed = null
  def Embed: DiscourseEmbed = this.embed

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def init(ws: WSClient) = {
    if (config.getBoolean("discourse.api.enabled").get) {
      val baseUrl = config.getString("discourse.baseUrl").get
      val apiKey = config.getString("discourse.api.key").get
      val categoryId = config.getInt("discourse.embed.categoryId").get
      this.users = new DiscourseUsers(baseUrl, ws)
      this.embed = new DiscourseEmbed(baseUrl, apiKey, categoryId, ws)
    } else {
      this.users = DiscourseUsers.Disabled
      this.embed = DiscourseEmbed.Disabled
    }
  }

  protected[forums] def validate[A](response: WSResponse)(f: JsObject => A): Option[A] = try {
    val json = response.json.as[JsObject]
    if (!json.keys.contains("errors")) {
      Some(f(json))
    } else {
      None
    }
  } catch {
    case e: Exception => None
  }

}
