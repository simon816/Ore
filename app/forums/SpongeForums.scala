package forums

import javax.inject.Inject

import db.ModelService
import ore.UserBase
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

/**
  * Handles interactions between Ore and the Sponge forums.
  */
class SpongeForums @Inject()(val users: UserBase,
                             implicit val config: OreConfig,
                             implicit val env: OreEnv,
                             implicit val ws: WSClient,
                             implicit val service: ModelService) extends DiscourseApi {

  lazy val conf = config.forums
  lazy val URL = conf.getString("baseUrl").get

  override lazy val Auth = new DiscourseSSO(
    conf.getString("sso.url").get, conf.getString("sso.secret").get, users, this
  )

  override lazy val Users = new DiscourseUsers(URL, this)

  override lazy val Embed = new DiscourseEmbed(
    URL, conf.getString("api.key").get, conf.getInt("embed.categoryId").get, this
  )

}
