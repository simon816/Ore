package ore.project.util

import models.project.Version
import ore.Colors.Color
import play.api.cache.CacheApi
import util.{Cacheable, PendingAction}

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
case class PendingVersion(factory: ProjectFactory,
                          owner: String,
                          projectSlug: String,
                          var channelName: String,
                          var channelColor: Color,
                          version: Version,
                          plugin: PluginFile,
                          override val cacheApi: CacheApi)
                          extends PendingAction[Version]
                            with Cacheable {

  override def complete: Try[Version] = Try {
    free()
    return factory.createVersion(this)
  }

  override def cancel() = {
    free()
    this.plugin.delete()
    if (this.version.isDefined) this.factory.deleteVersion(this.version)
  }

  override def key: String = this.owner + '/' + this.projectSlug + '/' + this.version.versionString

}
