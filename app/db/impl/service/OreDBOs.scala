package db.impl.service

import db.ModelService
import db.impl.access._
import ore.{OreConfig, OreEnv}

trait OreDBOs extends ModelService {

  val env: OreEnv
  val config: OreConfig

  val Users         = new UserBase()(this, this.config)
  val Projects      = new ProjectBase()(this, this.env, this.config)
  val Organizations = new OrganizationBase()(this, this.config)

}
