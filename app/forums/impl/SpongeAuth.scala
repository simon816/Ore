package forums.impl

import javax.inject.Inject

import db.ModelService
import db.impl.UserBase
import forums.{DiscourseApi, DiscourseSSO}
import util.OreConfig

/**
  * Sponge forums authentication service.
  *
  * @param config Ore config
  * @param api    DiscourseApi
  */
class SpongeAuth @Inject()(config: OreConfig,
                           service: ModelService,
                           override val api: DiscourseApi)
                           extends DiscourseSSO {
  override protected val users = service.access(classOf[UserBase])
  override val url = config.forums.getString("sso.url").get
  override val secret = config.forums.getString("sso.secret").get
}
