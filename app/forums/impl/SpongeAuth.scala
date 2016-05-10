package forums.impl

import javax.inject.Inject

import forums.{DiscourseApi, DiscourseSSO}
import ore.UserBase
import util.OreConfig

/**
  * Sponge forums authentication service.
  *
  * @param config Ore config
  * @param api    DiscourseApi
  * @param users  UserBase
  */
class SpongeAuth @Inject()(config: OreConfig,
                           val api: DiscourseApi,
                           override val users: UserBase) extends DiscourseSSO {
  override val url = config.forums.getString("sso.url")
  override val secret = config.forums.getString("sso.secret")
}
