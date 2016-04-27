package forums

import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSResponse}
import util.C._

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  lazy val Auth = new DiscourseSSO(DiscourseConf.getString("sso.url").get,
                                   DiscourseConf.getString("sso.secret").get)

  private var users: DiscourseUsers = null
  def Users: DiscourseUsers = this.users

  private var embed: DiscourseEmbed = null
  def Embed: DiscourseEmbed = this.embed

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def apply(implicit ws: WSClient) = {
    if (DiscourseConf.getBoolean("api.enabled").get) {
      val baseUrl = DiscourseConf.getString("baseUrl").get
      val apiKey = DiscourseConf.getString("api.key").get
      val categoryId = DiscourseConf.getInt("embed.categoryId").get
      this.users = new DiscourseUsers(baseUrl, ws)
      this.embed = new DiscourseEmbed(baseUrl, apiKey, categoryId, ws)
    } else disable()
  }

  def disable() = {
    this.users = DiscourseUsers.Disabled
    this.embed = DiscourseEmbed.Disabled
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
