package ore.project.factory

import db.impl.access.ProjectBase
import models.project.{Project, Version}
import ore.Cacheable
import ore.Colors.Color
import ore.project.io.PluginFile
import play.api.cache.CacheApi
import util.PendingAction

import scala.util.Try

/**
  * Represents a pending version to be created later.
  *
  * @param project        Project version belongs to
  * @param channelName    Name of channel this version will be in
  * @param channelColor   Color of channel for this version
  * @param version        Version that is pending
  * @param plugin         Uploaded plugin
  */
case class PendingVersion(projects: ProjectBase,
                          factory: ProjectFactory,
                          var project: Project,
                          var channelName: String,
                          var channelColor: Color,
                          version: Version,
                          plugin: PluginFile,
                          override val cacheApi: CacheApi)
                          extends PendingAction[Version]
                            with Cacheable {

  override def complete: Try[Version] = Try {
    free()
    return this.factory.createVersion(this)
  }

  override def cancel() = {
    free()
    this.plugin.delete()
    if (this.version.isDefined)
      this.projects.deleteVersion(this.version)
  }

  override def key: String = this.project.url + '/' + this.version.versionString

}
