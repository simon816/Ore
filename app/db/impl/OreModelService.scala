package db.impl

import javax.inject.Singleton

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.OreTypeSetters._
import db.impl.query.user.UserQueries
import db.impl.query.{PageQueries, ProjectQueries, VersionQueries}
import db.query.ModelQueries
import models.project.Channel
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason

@Singleton
class OreModelService extends ModelService {

  implicit val self = this
  import registrar.{register, registerSetter}

  registerSetter(classOf[Color], ColorTypeSetter)
  registerSetter(classOf[RoleType], RoleTypeTypeSetter)
  registerSetter(classOf[List[RoleType]], RoleTypeListTypeSetter)
  registerSetter(classOf[Category], CategoryTypeSetter)
  registerSetter(classOf[FlagReason], FlagReasonTypeSetter)

  register(new ModelQueries[ChannelTable, Channel](classOf[Channel], TableQuery[ChannelTable]))
  register(new PageQueries)
  register(new ProjectQueries)
  register(new UserQueries)
  register(new VersionQueries)

}
