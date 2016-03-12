package models.project

import java.sql.Timestamp

import db.Storage
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import plugin.{PluginManager, PluginFile}
import util.{Cacheable, PendingAction}

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}

/**
  * Represents a single version of a Project.
  *
  * @param id             Unique identifier
  * @param createdAt      Instant of creation
  * @param channelId      ID of channel this version belongs to
  * @param versionString  Version string
  */
case class Version(id: Option[Int], var createdAt: Option[Timestamp], projectId: Int, var channelId: Int, versionString: String) {

  def this(projectId: Int, channelId: Int, versionString: String) = this(None, None, projectId, channelId, versionString)

  def this(projectId: Int, versionString: String) = this(projectId, -1, versionString)

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

  /**
    * Returns true if this version already exists.
    *
    * @return True if version already exists, false otherwise
    */
  def exists: Boolean = {
    Storage.now(Storage.isDefined(Storage.getVersion(this.channelId, this.versionString))).isSuccess
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
                            version: Version, plugin: PluginFile) extends PendingAction with Cacheable {

    override def complete: Try[Unit] = Try {
      free()
      // Get project
      Storage.now(Storage.getProject(this.owner, this.projectName)) match {
        case Failure(thrown) =>
          cancel()
          throw thrown
        case Success(project) =>
          // Get channel
          var channel: Channel = null
          Storage.now(project.getChannel(this.channelName)) match {
            case Failure(thrown) =>
              cancel()
              throw thrown
            case Success(channelOpt) => channelOpt match {
              case None =>
                // Create new channel
                Storage.now(project.newChannel(this.channelName)) match {
                  case Failure(thrown) =>
                    cancel()
                    throw thrown
                  case Success(newChannel) => channel = newChannel
                }
              case Some(existing) => channel = existing
            }
          }

          // Upload plugin
          PluginManager.uploadPlugin(this.plugin) match {
            case Failure(thrown) =>
              cancel()
              throw thrown
            case Success(void) =>
              // Create version
              this.version.channelId = channel.id.get
              Storage.now(Storage.createVersion(this.version)) match {
                case Failure(thrown) =>
                  cancel()
                  throw thrown
                case _ => ;
              }
          }
      }
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
  def setPending(owner: String, name: String, channel: String, version: Version, plugin: PluginFile): Unit = {
    val pending = PendingVersion(owner, name, channel, version, plugin)
    Cache.set(pending.getKey, pending)
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
  def getPending(owner: String, name: String, channel: String, version: String) = {
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
    new Version(project.id.get, meta.getVersion)
  }

}
