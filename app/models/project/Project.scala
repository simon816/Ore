package models.project

import java.sql.Timestamp

import db.Storage
import models.author.Author
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import plugin.{PluginManager, PluginFile}
import util.{PendingAction, Cacheable}

import scala.concurrent.Future
import scala.util.{Success, Failure, Try}

/**
  * Represents an Ore package.
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * <p>Note: Instance variables should be private unless they are database
  * properties</p>
  *
  * @param id          Unique identifier
  * @param createdAt   Instant of creation
  * @param pluginId    Plugin ID
  * @param name        Name of plugin
  * @param description Short description of plugin
  * @param owner       The owner Author for this project
  * @param views       How many times this project has been views
  * @param downloads   How many times this project has been downloaded in total
  * @param starred     How many times this project has been starred
  */
case class Project(id: Option[Int], var createdAt: Option[Timestamp], pluginId: String, name: String, description: String,
                   owner: String, var recommendedVersionId: Option[Int], views: Int, downloads: Int, starred: Int) {

  def this(pluginId: String, name: String, description: String, owner: String) = {
    this(None, None, pluginId, name, description, owner, None, 0, 0, 0)
  }

  def getOwner: Author = throw new NotImplementedError // TODO

  /**
    * Returns all Channels belonging to this Project.
    *
    * @return All channels in project
    */
  def getChannels: Future[Seq[Channel]] = Storage.getChannels(this.id.get)

  /**
    * Returns the Channel in this project with the specified name.
    *
    * @param name Name of channel
    * @return Channel with name, if present, None otherwise
    */
  def getChannel(name: String): Future[Option[Channel]] = Storage.optChannel(this.id.get, name)

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name Name of channel
    * @return New channel
    */
  def newChannel(name: String): Future[Channel] = {
    Storage.createChannel(new Channel(this.id.get, name))
  }

  /**
    * Returns this Project's recommended version.
    *
    * @return Recommended version
    */
  def getRecommendedVersion: Future[Version] = Storage.getVersion(this.recommendedVersionId.get)

  /**
    * Updates this project's recommended version.
    *
    * @param version Version to set
    * @return Result
    */
  def setRecommendedVersion(version: Version): Future[Int] = {
    Storage.updateProjectInt(this, table => table.recommendedVersionId, version.id.get, newId => {
      this.recommendedVersionId = Some(newId)
    })
  }

  /**
    * Returns all Versions belonging to this Project.
    *
    * @return All versions in project
    */
  def getVersions: Future[Seq[Version]] = Storage.getAllVersions(this.id.get)

  /**
    * Returns true if this Project already exists.
    *
    * @return True if project exists, false otherwise
    */
  def exists: Boolean = Storage.now(Storage.isDefined(Storage.getProject(this.owner, this.name))).isSuccess

  override def toString: String = "%s - %s".format(this.name, this.description)

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Project] && o.asInstanceOf[Project].id.get == this.id.get

}

object Project {

  /**
    * Represents a Project with an uploaded plugin that has not yet been
    * created.
    *
    * @param project Pending project
    * @param firstVersion Uploaded plugin
    */
  case class PendingProject(project: Project, firstVersion: PluginFile) extends PendingAction with Cacheable {

    override def complete: Try[Unit] = Try {
      free()
      // Upload plugin
      PluginManager.uploadPlugin(this.firstVersion) match {
        case Failure(thrown) =>
          cancel()
          throw thrown
        case Success(void) =>
          // Create project
          val meta = this.firstVersion.getMeta.get
          Storage.now(Storage.createProject(this.project)) match {
            case Failure(thrown) =>
              cancel()
              throw thrown
            case Success(newProject) =>
              // Create first channel
              val channelName = Channel.getSuggestedNameForVersion(meta.getVersion)
              Storage.now(newProject.newChannel(channelName)) match {
                case Failure(thrown) =>
                  cancel()
                  throw thrown
                case Success(channel) =>
                  // Create first version
                  Storage.now(channel.newVersion(meta.getVersion)) match {
                    case Failure(thrown) =>
                      cancel()
                      throw thrown
                    case Success(newVersion) =>
                      newProject.setRecommendedVersion(newVersion)
                  }
              }
          }
      }
    }

    override def cancel() = {
      this.firstVersion.delete()
      if (project.exists) {
        // TODO: Delete project
      }
    }

    override def getKey: String = this.project.owner + '/' + this.project.name

  }

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project Project that is pending
    * @param firstVersion Uploaded plugin
    */
  def setPending(project: Project, firstVersion: PluginFile) =  {
    PendingProject(project, firstVersion).cache()
  }

  /**
    * Returns the PendingProject of the specified owner and name, if any.
    *
    * @param owner Project owner
    * @param name Project name
    * @return PendingProject if present, None otherwise
    */
  def getPending(owner: String, name: String): Option[PendingProject] = {
    Cache.getAs[PendingProject](owner + '/' + name)
  }

  /**
    * Creates a new Project from the specified PluginMetadata.
    *
    * @param owner Owner of project
    * @param meta PluginMetadata object
    * @return New project
    */
  def fromMeta(owner: String, meta: PluginMetadata): Project = {
    val desc = if (meta.getDescription != null) meta.getDescription else "" // TODO: Disallow null descriptions
    new Project(meta.getId, meta.getName, desc, owner)
  }

}
