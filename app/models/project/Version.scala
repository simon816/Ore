package models.project

import java.sql.Timestamp
import java.text.SimpleDateFormat

import db.Model
import db.query.Queries
import db.query.Queries.now
import models.project.Version._
import org.spongepowered.plugin.meta.PluginMetadata
import ore.Colors.Color
import ore._
import play.api.Play.current
import play.api.cache.Cache
import util.{Cacheable, PendingAction}

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Represents a single version of a Project.
  *
  * @param id               Unique identifier
  * @param createdAt        Instant of creation
  * @param versionString    Version string
  * @param dependenciesIds  List of plugin dependencies with the plugin ID and
  *                         version separated by a ':'
  * @param _description     User description of version
  * @param assets           Path to assets directory within plugin
  * @param _downloads       The amount of times this version has been downloaded
  * @param projectId        ID of project this version belongs to
  * @param channelId        ID of channel this version belongs to
  */
case class Version(override val id: Option[Int], override val createdAt: Option[Timestamp],
                   versionString: String, dependenciesIds: List[String],
                   private var _description: Option[String], assets: Option[String],
                   private var _downloads: Int, projectId: Int, var channelId: Int) extends Model {

  def this(versionString: String, dependencies: List[String], description: String,
           assets: String, projectId: Int, channelId: Int) = {
    this(None, None, versionString, dependencies, Option(description), Option(assets), 0, projectId, channelId)
  }

  def this(versionString: String, dependencies: List[String],
           description: String, assets: String, projectId: Int) = {
    this(versionString, dependencies, description, assets, projectId, -1)
  }

  /**
    * Returns the project this version belongs to.
    *
    * @return Project
    */
  def project: Project = Project.withId(this.projectId).get

  /**
    * Returns the channel this version belongs to.
    *
    * @return Channel
    */
  def channel: Channel = now(Queries.Channels.get(this.channelId)).get.get

  /**
    * Returns the channel this version belongs to from the specified collection
    * of channels if present.
    *
    * @param channels   Channels to search
    * @return           Channel if present, None otherwise
    */
  def findChannelFrom(channels: Seq[Channel]): Option[Channel] = channels.find(_.id.get == this.channelId)

  /**
    * Returns this Version's description.
    *
    * @return Version description
    */
  def description: Option[String] = this._description

  /**
    * Sets this Version's description.
    *
    * @param _description Version description
    */
  def description_=(_description: String) = {
    now(Queries.Versions.setString(this, _.description, _description)).get
    this._description = Some(_description)
  }

  /**
    * Returns this Versions plugin dependencies.
    *
    * @return Plugin dependencies
    */
  def dependencies: List[Dependency] = {
    for (depend <- this.dependenciesIds) yield {
      val data = depend.split(":")
      Dependency(data(0), data(1))
    }
  }

  /**
    * Returns the amount of unique downloads this Version has.
    *
    * @return Amount of unique downloads
    */
  def downloads: Int = this._downloads

  /**
    * Increments this Version's download count by one.
    *
    * @return Future result
    */
  def addDownload() = {
    now(Queries.Versions.setInt(this, _.downloads, this._downloads + 1)).get
    this._downloads += 1
  }

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Version] && o.asInstanceOf[Version].id.get == this.id.get
  }

}

object Version {
  
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
  case class PendingVersion(owner: String, projectSlug: String, var channelName: String,
                            var channelColor: Color, version: Version,
                            plugin: PluginFile) extends PendingAction[Version] with Cacheable {

    override def complete: Try[Version] = Try {
      free()
      return ProjectManager.createVersion(this)
    }
    
    override def cancel() = {
      free()
      this.plugin.delete()
    }

    override def key: String = {
      this.owner + '/' + this.projectSlug + '/' + this.channelName + '/' + this.version.versionString
    }

  }

  /**
    * Marks the specified Version as pending and caches it for later use.
    *
    * @param owner    Name of owner
    * @param slug     Project slug
    * @param channel  Name of channel
    * @param version  Name of version
    * @param plugin   Uploaded plugin
    */
  def setPending(owner: String, slug: String, channel: String, version: Version, plugin: PluginFile): PendingVersion = {
    val pending = PendingVersion(owner, slug, channel, Channel.DEFAULT_COLOR, version, plugin)
    pending.cache()
    pending
  }

  /**
    * Returns the pending version for the specified owner, name, channel, and
    * version string.
    *
    * @param owner    Name of owner
    * @param slug     Project slug
    * @param channel  Name of channel
    * @param version  Name of version
    * @return         PendingVersion, if present, None otherwise
    */
  def getPending(owner: String, slug: String, channel: String, version: String): Option[PendingVersion] = {
    Cache.getAs[PendingVersion](owner + '/' + slug + '/' + channel + '/' + version)
  }

  /**
    * Creates a new Version from the specified PluginMetadata.
    *
    * @param project  Project this version belongs to
    * @param meta     PluginMetadata
    * @return         New Version
    */
  def fromMeta(project: Project, meta: PluginMetadata): Version = {
    // TODO: asset parsing
    val depends = for (depend <- meta.getRequiredDependencies) yield depend.getId + ":" + depend.getVersion
    new Version(meta.getVersion, depends.toList, meta.getDescription, "", project.id.getOrElse(-1))
  }

}
