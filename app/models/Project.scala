package models

import models.author.{Author, Dev}
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache

import scala.collection.JavaConversions._

/**
  * Represents an Ore package. The specified ID should correspond with the
  * actual plugin id defined in the meta file. (TODO: check meta file in file
  * uploads).
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * <p>Note: Instance variables should be private unless they are database
  * properties</p>
  *
  * TODO: Versions / channels
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
  var views = 0

  /**
    * The amount of times this Project has been downloaded.
    *
    * TODO: Unique downloads?
    */
  var downloads = 0

  /**
    * The amount of users who have starred this project.
    */
  var starred = 0

  private var pendingUpload: Option[PluginFile] = None

  def this(id: String, name: String, description: String, owner: Author) = this(id, name, description, owner, List(owner))

  /**
    * Returns how this Project is represented in the Cache.
    *
    * @return Key of cache
    */
  def getKey = this.owner.name + '/' + this.name

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
  def getPendingUpload = this.pendingUpload

  override def toString = "%s - %s".format(this.name, this.description)

}

/**
  * Project data-store
  */
object Project {

  // TODO: Replace with DB
  var projects = Seq(
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
  def get(owner: String, name: String): Option[Project] = {
    for (project <- projects) {
      if (project.owner.name.equals(owner) && project.name.equals(name)) {
        return Some(project)
      }
    }
    None
  }

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
    val devs = for (author <- meta.getAuthors.toList) yield Dev(author)
    Project(meta.getId, meta.getName, meta.getDescription, owner, devs)
  }

}
