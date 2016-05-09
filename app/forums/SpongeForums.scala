package forums

import javax.inject.Inject

import db.ModelService
import play.api.libs.ws.WSClient
import util.Conf.DiscourseConf

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(implicit val ws: WSClient, implicit val service: ModelService) extends DiscourseApi {

  lazy val URL = DiscourseConf.getString("baseUrl").get

  override lazy val Auth = new DiscourseSso(
    DiscourseConf.getString("sso.url").get, DiscourseConf.getString("sso.secret").get, this)

  override lazy val Users = new DiscourseUsers(URL, this)

  override lazy val Embed = new DiscourseEmbed(
    URL, DiscourseConf.getString("api.key").get, DiscourseConf.getInt("embed.categoryId").get, this
  )

}
