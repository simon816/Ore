package db.impl.service

import db.ModelService
import db.impl.access._
import discourse.OreDiscourseApi
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi
import security.spauth.SpongeAuthApi

trait OreDBOs extends ModelService {

  val env: OreEnv
  val forums: OreDiscourseApi
  val auth: SpongeAuthApi
  val config: OreConfig
  val messages: MessagesApi

  val Users = new UserBase(this, this.auth, this.config)
  val Projects = new ProjectBase(this, this.env, this.config, this.forums)
  val Organizations = new OrganizationBase(this, this.forums, this.auth, this.config, this.messages, Users)

}
