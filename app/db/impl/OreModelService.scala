package db.impl

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import db.impl.OrePostgresDriver.api._
import db.impl.access.{FlagBase, ProjectBase, UserBase, VersionBase, _}
import db.impl.schema.{PageSchema, ProjectSchema, UserSchema, VersionSchema}
import db.{ModelAssociation, ModelRegistry, ModelSchema, ModelService}
import forums.DiscourseApi
import models.project._
import models.statistic.{ProjectView, VersionDownload}
import models.user.role.{OrganizationRole, ProjectRole}
import models.user.{Notification, Organization, User}
import ore.{OreConfig, OreEnv}
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.MessagesApi
import slick.driver.JdbcProfile

import scala.concurrent.duration.Duration

/**
  * The Ore ModelService implementation. Contains registration of Ore-specific
  * types and Models.
  *
  * @param db DatabaseConfig
  */
@Singleton
class OreModelService @Inject()(env: OreEnv,
                                config: OreConfig,
                                forums: DiscourseApi,
                                db: DatabaseConfigProvider,
                                messages: MessagesApi)
                                extends ModelService {

  override lazy val registry = new ModelRegistry {}
  override lazy val processor = new OreModelProcessor(
    this, this.users, this.projects, this.orgs, this.config, this.forums
  )
  override lazy val driver = OrePostgresDriver
  override lazy val DB = db.get[JdbcProfile]
  override lazy val DefaultTimeout: Duration = Duration(config.app.getInt("db.default-timeout").get, TimeUnit.SECONDS)

  import registry.{registerSchema, registerModelBase}

  val users = registerModelBase(classOf[UserBase], new UserBase(this, forums, config))
  val projects = registerModelBase(classOf[ProjectBase], new ProjectBase(this, this.env, this.config, this.forums))
  val versions = registerModelBase(classOf[VersionBase], new VersionBase(this))
  val flags = registerModelBase(classOf[FlagBase], new FlagBase(this))
  val orgs = registerModelBase[OrganizationBase](classOf[OrganizationBase], new OrganizationBase(
    this, this.forums, this.config, this.messages
  ))

  // Associations
  val projectWatchers = new ModelAssociation[ProjectWatchersTable](
    this, _.projectId, _.userId, classOf[ProjectWatchersTable], TableQuery[ProjectWatchersTable])

  val projectMembers = new ModelAssociation[ProjectMembersTable](
    this, _.projectId, _.userId, classOf[ProjectMembersTable], TableQuery[ProjectMembersTable])

  val organizationMembers = new ModelAssociation[OrganizationMembersTable](
    this, _.userId, _.organizationId, classOf[OrganizationMembersTable], TableQuery[OrganizationMembersTable])

  val stars = new ModelAssociation[ProjectStarsTable](
    this, _.userId, _.projectId, classOf[ProjectStarsTable], TableQuery[ProjectStarsTable])

  // User schema
  registerSchema(new UserSchema(this))
    .withChildren[Project](classOf[Project], _.userId)
    .withChildren[ProjectRole](classOf[ProjectRole], _.userId)
    .withChildren[OrganizationRole](classOf[OrganizationRole], _.userId)
    .withChildren[Flag](classOf[Flag], _.userId)
    .withChildren[Notification](classOf[Notification], _.userId)
    .withChildren[Organization](classOf[Organization], _.userId)
    .withAssociation[ProjectWatchersTable, Project](
      association = this.projectWatchers,
      selfReference = _.userId,
      targetClass = classOf[Project],
      targetReference = _.projectId)
    .withAssociation[ProjectMembersTable, Project](
      association = this.projectMembers,
      selfReference = _.userId,
      targetClass = classOf[Project],
      targetReference = _.projectId)
    .withAssociation[OrganizationMembersTable, Organization](
      association = this.organizationMembers,
      selfReference = _.userId,
      targetClass = classOf[Organization],
      targetReference = _.organizationId)
    .withAssociation[ProjectStarsTable, Project](
      association = this.stars,
      selfReference = _.userId,
      targetClass = classOf[Project],
      targetReference = _.projectId)

  // Project schema
  registerSchema(new ProjectSchema(this))
    .withChildren[Channel](classOf[Channel], _.projectId)
    .withChildren[Version](classOf[Version], _.projectId)
    .withChildren[Page](classOf[Page], _.projectId)
    .withChildren[Flag](classOf[Flag], _.projectId)
    .withChildren[ProjectRole](classOf[ProjectRole], _.projectId)
    .withChildren[ProjectView](classOf[ProjectView], _.modelId)
    .withAssociation[ProjectWatchersTable, User](
      association = this.projectWatchers,
      selfReference = _.projectId,
      targetClass = classOf[User],
      targetReference = _.userId)
    .withAssociation[ProjectMembersTable, User](
      association = this.projectMembers,
      selfReference = _.projectId,
      targetClass = classOf[User],
      targetReference = _.userId)
    .withAssociation[ProjectStarsTable, User](
      association = this.stars,
      selfReference = _.projectId,
      targetClass = classOf[User],
      targetReference = _.userId)

  registerSchema(new VersionSchema(this)).withChildren[VersionDownload](classOf[VersionDownload], _.modelId)

  registerSchema(new ModelSchema[Channel](this, classOf[Channel], TableQuery[ChannelTable]))
    .withChildren[Version](classOf[Version], _.channelId)

  registerSchema(new PageSchema(this))

  registerSchema(new ModelSchema[Notification](this, classOf[Notification], TableQuery[NotificationTable]))

  registerSchema(new ModelSchema[Organization](this, classOf[Organization], TableQuery[OrganizationTable]))
    .withChildren[Project](classOf[Project], _.userId)
    .withChildren[OrganizationRole](classOf[OrganizationRole], _.organizationId)
    .withAssociation[OrganizationMembersTable, User](
      association = this.organizationMembers,
      selfReference = _.organizationId,
      targetClass = classOf[User],
      targetReference = _.userId)

  registerSchema(new ModelSchema[OrganizationRole](this, classOf[OrganizationRole], TableQuery[OrganizationRoleTable]))

}
