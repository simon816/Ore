package db.model

import db.ChannelTable
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries.registrar.register
import db.query._
import db.query.impl.user.UserQueries
import db.query.impl.{PageQueries, ProjectQueries, VersionQueries}
import models.project.Channel

object Models {

  val Channels  =   register(new ModelQueries[ChannelTable, Channel](classOf[Channel], TableQuery[ChannelTable]))
  val Pages     =   register(new PageQueries)
  val Projects  =   register(new ProjectQueries)
  val Users     =   register(new UserQueries)
  val Versions  =   register(new VersionQueries)

}
