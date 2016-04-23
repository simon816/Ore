package models.project

import java.sql.Timestamp

import db.orm.dao.ModelDAO
import db.orm.model.ModelKeys._
import db.orm.model.NamedModel
import db.query.Queries
import db.query.Queries.now
import ore.Colors.Color
import ore.permission.scope.ProjectScope
import ore.project.{Dependency, PluginFile, ProjectFactory}
import org.apache.commons.io.FileUtils
import play.api.Play.current
import play.api.cache.Cache
import util.Input._
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
case class Version(override val   id: Option[Int] = None,
                   override val   createdAt: Option[Timestamp] = None,
                   val            versionString: String,
                   val            dependenciesIds: List[String] = List(),
                   private var    _description: Option[String] = None,
                   val            assets: Option[String] = None,
                   private var    _downloads: Int = 0,
                   override val   projectId: Int,
                   val            channelId: Int,
                   val            fileSize: Long,
                   val            hash: String)
                   extends        NamedModel
                   with           ProjectScope { self =>

  def this(versionString: String, dependencies: List[String], description: String,
           assets: String, projectId: Int, channelId: Int, fileSize: Long, hash: String) = {
    this(None, None, versionString, dependencies, Option(description),
         Option(assets), 0, projectId, channelId, fileSize, hash)
  }

  def this(versionString: String, dependencies: List[String],
           description: String, assets: String, projectId: Int, fileSize: Long, hash: String) = {
    this(versionString, dependencies, description, assets, projectId, -1, fileSize, hash)
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
  def findChannelFrom(channels: Seq[Channel]): Option[Channel] = assertDefined {
    channels.find(_.id.get == this.channelId)
  }

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
    this._description = Some(_description)
    if (isDefined) update(Description)
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
    this._downloads += 1
    if (isDefined) update(Downloads)
  }

  /**
    * Returns a human readable file size for this Version.
    *
    * @return Human readable file size
    */
  def humanFileSize: String = FileUtils.byteCountToDisplaySize(this.fileSize)

  def exists: Boolean = {
    this.projectId > -1 &&
      ((this.channelId > -1 && this.channel.versions.withName(this.versionString).isDefined) ||
      now(Queries.Versions.hashExists(this.projectId, this.hash)).get)
  }

  override def name: String = this.versionString

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Version] && o.asInstanceOf[Version].id.get == this.id.get
  }

  // Table bindings

  override type M <: Version { type M = self.M }

  bind[String](Description, _._description.orNull, description => {
    Seq(Queries.Versions.setString(this, _.description, description))
  })
  bind[Int](Downloads, _._downloads, downloads => Seq(Queries.Versions.setInt(this, _.downloads, downloads)))

}

object Version extends ModelDAO[Version] {
  
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
  case class PendingVersion(val       owner: String,
                            val       projectSlug: String,
                            var       channelName: String,
                            var       channelColor: Color,
                            val       version: Version,
                            val       plugin: PluginFile)
                            extends   PendingAction[Version]
                            with      Cacheable {

    override def complete: Try[Version] = Try {
      free()
      return ProjectFactory.createVersion(this)
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
    val pending = PendingVersion(owner, slug, channel, Channel.DefaultColor, version, plugin)
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
    * @param plugin   PluginFile
    * @return         New Version
    */
  def fromMeta(project: Project, plugin: PluginFile): Version = {
    // TODO: asset parsing
    val meta = plugin.meta.get
    val depends = for (depend <- meta.getRequiredDependencies) yield depend.getId + ":" + depend.getVersion
    val path = plugin.path
    new Version(
      meta.getVersion, depends.toList, meta.getDescription, "",
      project.id.getOrElse(-1), path.toFile.length, md5(path)
    )
  }

  override def withId(id: Int): Option[Version] = now(Queries.Versions.get(id)).get

}
