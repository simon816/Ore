package models.project

import models.author.{Team, Author, Dev}
import util.PluginFile
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache

import scala.collection.JavaConversions._

/**
  * Represents an Ore package.
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * <p>Note: Instance variables should be private unless they are database
  * properties</p>
  *
  * TODO: Per-version descriptions
  *
  * @param id          Plugin ID
  * @param name        Name of plugin
  * @param description Short description of plugin
  * @param owner       The owner Author for this project
  * @param authors     List of addition authors on this project
  */
case class Project(id: String, name: String, description: String, owner: Author, authors: List[Author]) {

  /**
    * The amount of times this Project has been viewed.
    *
    * TODO: Unique views?
    */
  val views: Int = 0

  /**
    * The amount of times this Project has been downloaded.
    *
    * TODO: Unique downloads?
    */
  val downloads: Int = 0

  /**
    * The amount of users who have starred this project.
    */
  val starred: Int = 0

  private var pendingUpload: Option[PluginFile] = None

  def this(id: String, name: String, description: String, owner: Author) = {
    this(id, name, description, owner, List(owner))
  }

  /**
    * Returns how this Project is represented in the Cache.
    *
    * @return Key of cache
    */
  def getKey: String = this.owner.name + '/' + this.name

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
  def exists: Boolean = false // TODO

  /**
    * Creates this Project
    *
    * TODO: Add to DB here
    */
  def create() = if (exists) throw new Exception("This project already exists.")

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

  /**
    * Returns the Channel in this project with the specified name.
    *
    * @param name Name of channel
    * @return Channel with name, if present, None otherwise
    */
  def getChannel(name: String): Option[Channel] = Channel.get(this, name)

  /**
    * Returns all Channels belonging to this Project.
    *
    * @return All channels in project
    */
  def getChannels: Set[Channel] = Channel.getAll(this)

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name Name of channel
    * @return New channel
    */
  def newChannel(name: String): Channel = new Channel(this, name) // TODO: Add channel to DB here

  /**
    * Returns all Versions belonging to this Project.
    *
    * @return All versions in project
    */
  def getVersions: Set[Version] = Version.getAll(this)

  override def toString: String = "%s - %s".format(this.name, this.description)

  override def hashCode: Int = getKey.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Project] && o.asInstanceOf[Project].getKey.equals(getKey)

}

/**
  * Project data-store
  */
object Project {

  // TODO: Replace with DB
  val projects = Set[Project](
    new Project("org.spongepowered.ore", "Ore", "The Minecraft Package Repository", Team.get("SpongePowered").get),
    new Project("example1", "Example-1", "Description 1", Dev.get("Author1").get),
    new Project("example2", "Example-2", "Description 2", Dev.get("Author2").get),
    new Project("example3", "Example-3", "Description 3", Dev.get("Author3").get),
    new Project("example4", "Example-4", "Description 4", Dev.get("Author4").get),
    new Project("example5", "Example-5", "Description 5", Dev.get("Author5").get)
  )

  /**
    * Returns the project with the specified owner and name.
    *
    * @param owner Project owner
    * @param name Project name
    * @return Project if exists, None otherwise
    */
  def get(owner: Author, name: String): Option[Project] = {
    this.projects.find(project => project.owner.equals(owner) && project.name.equals(name))
  }

  /**
    * Returns all Projects by the specified Author.
    *
    * @param owner Owner of projects
    * @return Set of projects owned by specified author
    */
  def getAll(owner: Author): Set[Project] = this.projects.filter(project => project.owner.equals(owner))

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
  def fromMeta(owner: Author, meta: PluginMetadata): Project = {
    val devs = for (author <- meta.getAuthors.toList) yield Author.get(author)
    new Project(meta.getId, meta.getName, meta.getDescription, owner, devs)
  }

}
