package ore.project.util

import db.ModelService
import forums.DiscourseApi
import models.project.{Channel, Version}
import ore.Colors.Color
import play.api.cache.CacheApi
import util.{Cacheable, OreConfig, PendingAction}

import scala.util.Try

/**
  * Represents a pending version to be created later.
  *
  * @param owner          Name of project owner
  * @param projectSlug    Project slug
  * @param channelName    Name of channel this version will be in
  * @param channelColor   Color of channel for this version
  * @param version        Version that is pending
  * @param plugin         Uploaded plugin
  */
case class PendingVersion(owner: String,
                          projectSlug: String,
                          var channelName: String,
                          var channelColor: Color,
                          version: Version,
                          plugin: PluginFile)
                         (implicit service: ModelService,
                          forums: DiscourseApi,
                          factory: ProjectFactory,
                          config: OreConfig,
                          override val cacheApi: CacheApi)
                          extends PendingAction[Version] with Cacheable {

  this.channelColor = Channel.DefaultColor
  this.channelName = Channel.DefaultName

  implicit val fileManager = factory.fileManager

  override def complete: Try[Version] = Try {
    free()
    return factory.createVersion(this)
  }

  override def cancel() = {
    free()
    this.plugin.delete()
    if (this.version.isDefined) this.version.delete()
  }

  override def key: String = {
    this.owner + '/' + this.projectSlug + '/' + this.version.versionString
  }

}
