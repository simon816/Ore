package db.impl

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import db.{ModelRegistrar, ModelService}
import db.impl.OrePostgresDriver.api._
import db.impl.OreTypeSetters._
import db.impl.action.user.UserActions
import db.impl.action.{PageActions, ProjectActions, VersionActions}
import db.action.ModelActions
import db.meta.BindingsGenerator
import models.project.Channel
import ore.Colors.Color
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason
import play.api.db.slick.DatabaseConfigProvider
import slick.driver.JdbcProfile
import util.Conf._

import scala.concurrent.duration.Duration

@Singleton
class OreModelService @Inject()(override val registrar: ModelRegistrar,
                                override val bindingsGenerator: BindingsGenerator,
                                db: DatabaseConfigProvider) extends ModelService {

  override lazy val DB = db.get[JdbcProfile]
  override lazy val DefaultTimeout: Duration = Duration(AppConf.getInt("db.default-timeout").get, TimeUnit.SECONDS)

  implicit val self = this
  import registrar.{register, registerSetter}

  registerSetter(classOf[Color], ColorTypeSetter)
  registerSetter(classOf[RoleType], RoleTypeTypeSetter)
  registerSetter(classOf[List[RoleType]], RoleTypeListTypeSetter)
  registerSetter(classOf[Category], CategoryTypeSetter)
  registerSetter(classOf[FlagReason], FlagReasonTypeSetter)

  register(new ModelActions[ChannelTable, Channel](classOf[Channel], TableQuery[ChannelTable]))
  register(new PageActions)
  register(new ProjectActions)
  register(new UserActions)
  register(new VersionActions)

}
