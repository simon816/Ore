package models.project

import java.sql.Timestamp

import models.author.Author
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import db.Storage
import plugin.PluginFile

import scala.concurrent.Future

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
                   owner: String, views: Int, downloads: Int, starred: Int) {

  def this(pluginId: String, name: String, description: String, owner: String) = {
    this(None, None, pluginId, name, description, owner, 0, 0, 0)
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
    * Returns all Versions belonging to this Project.
    *
    * @return All versions in project
    */
  def getVersions: Future[Seq[Version]] = Storage.getAllVersions(this.id.get)

  /**
    * Returns how this Project is represented in the Cache.
    *
    * @return Key of cache
    */
  def getKey: String = this.owner + '/' + this.name

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

  case class PendingProject(project: Project, firstVersion: PluginFile) {

    def free() = Cache.remove(project.getKey)

  }

  def setPending(project: Project, firstVersion: PluginFile) =  {
    Cache.set(project.getKey, PendingProject(project, firstVersion))
  }

  def getPending(author: String, name: String): Option[PendingProject] = {
    Cache.getAs[PendingProject](author + '/' + name)
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
