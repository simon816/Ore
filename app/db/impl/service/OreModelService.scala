package db.impl.service

import javax.inject.{Inject, Singleton}

import db.impl.OrePostgresDriver.api._
import db.impl.{OreModelProcessor, OrePostgresDriver}
import db.{ModelRegistry, ModelService}
import discourse.OreDiscourseApi
import ore.{OreConfig, OreEnv}
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.MessagesApi
import security.spauth.SpongeAuthApi
import slick.driver.JdbcProfile

import scala.concurrent.duration._

/**
  * The Ore ModelService implementation. Contains registration of Ore-specific
  * types and Models.
  *
  * @param db DatabaseConfig
  */
@Singleton
class OreModelService @Inject()(override val env: OreEnv,
                                override val config: OreConfig,
                                override val forums: OreDiscourseApi,
                                override val auth: SpongeAuthApi,
                                override val messages: MessagesApi,
                                db: DatabaseConfigProvider)
                                extends ModelService with OreModelConfig {

  val Logger = play.api.Logger("Database")

  // Implement ModelService
  override lazy val registry = new ModelRegistry {}
  override lazy val processor = new OreModelProcessor(
    this, Users, Projects, Organizations, this.config, this.forums, this.auth)
  override lazy val driver = OrePostgresDriver
  override lazy val DB = db.get[JdbcProfile]
  override lazy val DefaultTimeout: Duration = this.config.app.getInt("db.default-timeout").get.seconds

  import registry.{registerModelBase, registerSchema}

  override def start() = {
    val time = System.currentTimeMillis()

    // Initialize database access objects
    registerModelBase(Users)
    registerModelBase(Projects)
    registerModelBase(Organizations)

    // Register model schemas
    registerSchema(UserSchema)
    registerSchema(SessionSchema)
    registerSchema(SignOnSchema)
    registerSchema(ProjectRolesSchema)
    registerSchema(ProjectSchema)
    registerSchema(ProjectSettingsSchema)
    registerSchema(ProjectLogSchema)
    registerSchema(ProjectLogEntrySchema)
    registerSchema(FlagSchema)
    registerSchema(ViewSchema)
    registerSchema(VersionSchema)
    registerSchema(DownloadWarningSchema)
    registerSchema(UnsafeDownloadSchema)
    registerSchema(DownloadSchema)
    registerSchema(ChannelSchema)
    registerSchema(PageSchema)
    registerSchema(NotificationSchema)
    registerSchema(OrganizationSchema)
    registerSchema(OrganizationRoleSchema)
    registerSchema(ProjectApiKeySchema)

    Logger.info(
      "Database initialized:\n" +
        s"Initialization time: ${System.currentTimeMillis() - time}ms\n" +
        s"Default timeout: ${DefaultTimeout.toString}\n" +
        s"Registered DBOs: ${this.registry.modelBases.size}\n" +
        s"Registered Schemas: ${this.registry.modelSchemas.size}")
  }

}
