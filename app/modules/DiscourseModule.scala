package modules

import com.google.inject.AbstractModule
import forums.{DiscourseApi, SpongeForums}

class DiscourseModule extends AbstractModule {
  def configure() = bind(classOf[DiscourseApi]).to(classOf[SpongeForums])
}
