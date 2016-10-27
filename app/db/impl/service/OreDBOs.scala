package db.impl.service

import db.ModelService
import db.impl.access._
import discourse.impl.OreDiscourseApi
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi

trait OreDBOs extends ModelService {

  val env: OreEnv
  val forums: OreDiscourseApi
  val config: OreConfig
  val messages: MessagesApi

  val Users = new UserBase(this, this.forums, this.config)
  val Projects = new ProjectBase(this, this.env, this.config, this.forums)
  val Organizations = new OrganizationBase(this, this.forums, this.config, this.messages, Users)

}
