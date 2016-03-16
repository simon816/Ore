package models.project

import java.sql.Timestamp
import java.text.SimpleDateFormat

import db.Storage
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import plugin.{PluginFile, ProjectManager}
import util.{Cacheable, PendingAction}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Represents a single version of a Project.
  *
  * @param id             Unique identifier
  * @param createdAt      Instant of creation
  * @param channelId      ID of channel this version belongs to
  * @param versionString  Version string
  */
case class Version(id: Option[Int], var createdAt: Option[Timestamp], versionString: String,
                   dependencies: List[String], description: Option[String], assets: Option[String],
                   downloads: Int, projectId: Int, var channelId: Int) {

  private val dateFormat = new SimpleDateFormat("MM-dd-yyyy")

  def this(versionString: String, dependencies: List[String], description: String, assets: String, projectId: Int, channelId: Int) = {
    this(None, None, versionString, dependencies, Option(description), Option(assets), 0, projectId, channelId)
  }

  def this(versionString: String, dependencies: List[String], description: String, assets: String, projectId: Int) = {
    this(versionString, dependencies, description, assets, projectId, -1)
  }

  /**
    * Returns the project this version belongs to.
    *
    * @return Project
    */
  def getProject: Future[Project] = Storage.getProject(this.projectId)

  /**
    * Returns the channel this version belongs to.
    *
    * @return Channel
    */
  def getChannel: Future[Channel] = Storage.getChannel(this.channelId)

  /**
    * Returns the channel this version belongs to from the specified collection
    * of channels if present.
    *
    * @param channels Channels to search
    * @return Channel if present, None otherwise
    */
  def getChannelFrom(channels: Seq[Channel]): Option[Channel] = channels.find(_.id.get == this.channelId)

  def prettyDate: String = {
    this.dateFormat.format(this.createdAt.get)
  }

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Version] && o.asInstanceOf[Version].id.get == this.id.get

}

object Version {

  /**
    * Represents a pending version to be created later.
    *
    * @param owner Name of project owner
    * @param projectName Name of project
    * @param channelName Name of channel this version will be in
    * @param version Version that is pending
    * @param plugin Uploaded plugin
    */
  case class PendingVersion(owner: String, projectName: String, channelName: String,
                            version: Version, plugin: PluginFile) extends PendingAction[Version] with Cacheable {

    override def complete: Try[Version] = Try {
      free()
      return ProjectManager.createVersion(this)
    }

    override def cancel() = {
      this.plugin.delete()
      Storage.now(Storage.isDefined(Storage.getProject(owner, projectName))) match {
        case Failure(thrown) => throw thrown
        case Success(exists) => // TODO: Delete project
      }
    }

    override def getKey: String = {
      this.owner + '/' + this.projectName + '/' + this.channelName + '/' + this.version.versionString
    }

  }

  /**
    * Marks the specified Version as pending and caches it for later use.
    *
    * @param owner Name of owner
    * @param name Name of project
    * @param channel Name of channel
    * @param version Name of version
    * @param plugin Uploaded plugin
    */
  def setPending(owner: String, name: String, channel: String, version: Version, plugin: PluginFile): PendingVersion = {
    val pending = PendingVersion(owner, name, channel, version, plugin)
    pending.cache()
    pending
  }

  /**
    * Returns the pending version for the specified owner, name, channel, and
    * version string.
    *
    * @param owner Name of owner
    * @param name Name of project
    * @param channel Name of channel
    * @param version Name of version
    * @return PendingVersion, if present, None otherwise
    */
  def getPending(owner: String, name: String, channel: String, version: String): Option[PendingVersion] = {
    Cache.getAs[PendingVersion](owner + '/' + name + '/' + channel + '/' + version)
  }

  /**
    * Creates a new Version from the specified PluginMetadata.
    *
    * @param project Project this version belongs to
    * @param meta PluginMetadata
    * @return New Version
    */
  def fromMeta(project: Project, meta: PluginMetadata): Version = {
    // TODO: Dependency parsing
    // TODO: asset parsing
    new Version(meta.getVersion, List(), meta.getDescription, "", project.id.getOrElse(-1))
  }

}
