package models.project

import java.sql.Timestamp

import models.author.Author
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import sql.Storage
import util.PluginFile

import scala.util.Try

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
case class Project(id: Int, createdAt: Timestamp, pluginId: String, name: String, description: String,
                   owner: String, views: Int, downloads: Int, starred: Int) {

  private var pendingUpload: Option[PluginFile] = None

  def this(pluginId: String, name: String, description: String, owner: String) = {
    this(-1, null, pluginId, name, description, owner, 0, 0, 0)
  }

  def getOwner: Author = Storage.getAuthor(this.owner)

  /**
    * Returns all Channels belonging to this Project.
    *
    * @return All channels in project
    */
  def getChannels: Seq[Channel] = Storage.getChannels(this.id)

  /**
    * Returns the Channel in this project with the specified name.
    *
    * @param name Name of channel
    * @return Channel with name, if present, None otherwise
    */
  def getChannel(name: String): Option[Channel] = Storage.getChannel(this.id, name)

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name Name of channel
    * @return New channel
    */
  def newChannel(name: String): Try[Channel] = {
    Storage.createChannel(new Channel(this.id, name))
  }

  /**
    * Returns all Versions belonging to this Project.
    *
    * @return All versions in project
    */
  def getVersions: Seq[Version] = Storage.getAllVersions(this.id)

  /**
    * Returns how this Project is represented in the Cache.
    *
    * @return Key of cache
    */
  def getKey: String = this.owner + '/' + this.name

  /**
    * Adds this Project to the cache, used to pass the model between requests
    * before the actual project has been created.
    *
    * TODO: Expiration
    */
  def cache() = Cache.set(getKey, this)

  /**
    * Removes this Project from the cache.
    */
  def free() = Cache.remove(getKey)

  /**
    * Returns true if this Project already exists.
    *
    * @return True if project exists, false otherwise
    */
  def exists: Boolean = Storage.getProject(this.name, this.owner).isDefined

  /**
    * Sets the PluginFile that is waiting to be uploaded.
    *
    * TODO: Expiration
    *
    * @param file To be uploaded
    */
  def setPendingUpload(file: PluginFile) = this.pendingUpload = Some(file)

  /**
    * Returns the PluginFile that is waiting to be uploaded.
    *
    * @return PluginFile waiting to be uploaded
    */
  def getPendingUpload: Option[PluginFile] = this.pendingUpload

  override def toString: String = "%s - %s".format(this.name, this.description)

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Project] && o.asInstanceOf[Project].id == this.id

}

object Project {

  /**
    * Gets the project with the specified owner and name from the Cache.
    *
    * @param owner Owner name
    * @param name project name
    * @return Project in cache, if any, None otherwise
    */
  def getCached(owner: String, name: String): Option[Project] = {
    Cache.getAs[Project](owner + '/' + name)
  }

  /**
    * Creates a new Project from the specified PluginMetadata.
    *
    * @param owner Owner of project
    * @param meta PluginMetadata object
    * @return New project
    */
  def fromMeta(owner: String, meta: PluginMetadata): Project = {
    new Project(meta.getId, meta.getName, meta.getDescription, owner)
  }

}
