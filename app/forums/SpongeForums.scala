package forums

import db.ModelService
import play.api.libs.json.JsObject
import play.api.libs.ws.{WSClient, WSResponse}
import util.Conf._

/**
  * Handles interactions between Ore and the Sponge forums.
  */
object SpongeForums {

  lazy val Auth = new DiscourseSSO(DiscourseConf.getString("sso.url").get,
                                   DiscourseConf.getString("sso.secret").get)

  var Users: DiscourseUsers = null
  var Embed: DiscourseEmbed = null

  /**
    * Initializes this object.
    *
    * @param ws HTTP request client
    */
  def enable()(implicit ws: WSClient, service: ModelService) = {
    if (DiscourseConf.getBoolean("api.enabled").get) {
      val baseUrl = DiscourseConf.getString("baseUrl").get
      val apiKey = DiscourseConf.getString("api.key").get
      val categoryId = DiscourseConf.getInt("embed.categoryId").get
      this.Users = new DiscourseUsers(baseUrl, ws)
      this.Embed = new DiscourseEmbed(baseUrl, apiKey, categoryId, ws, service)
    } else disable()
  }

  /** Disables SpongeForums access */
  def disable() = {
    this.Users = DiscourseUsers.Disabled
    this.Embed = DiscourseEmbed.Disabled
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
