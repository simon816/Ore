package models.project

import java.sql.Timestamp
import java.text.SimpleDateFormat

import com.google.common.base.Preconditions._
import db.Model
import db.query.Queries
import db.query.Queries.now
import models.project.Project._
import models.project.Version.PendingVersion
import models.project.author.Dev
import models.user.User
import org.apache.commons.io.FileUtils
import org.spongepowered.plugin.meta.PluginMetadata
import pkg.Categories.Category
import pkg.ChannelColors.ChannelColor
import pkg._
import play.api.Play.current
import play.api.cache.Cache
import util.{Cacheable, PendingAction}

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Represents an Ore package.
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * <p>Note: Instance variables should be private unless they are database
  * properties unless they are vars instead of vals in which case accessors
  * and mutators must be used.</p>
  *
  * @param id                     Unique identifier
  * @param createdAt              Instant of creation
  * @param pluginId               Plugin ID
  * @param _name                  Name of plugin
  * @param ownerName              The owner Author for this project
  * @param authorNames            Authors who work on this project
  * @param homepage               The external project URL
  * @param recommendedVersionId   The ID of this project's recommended version
  * @param categoryId             The ID of this project's category
  * @param _views                 How many times this project has been views
  * @param _downloads             How many times this project has been downloaded in total
  * @param _stars                 How many times this project has been starred
  * @param _issues                External link to issue tracker
  * @param _source                External link to source code
  */
case class Project(override val id: Option[Int], override val createdAt: Option[Timestamp],
                   pluginId: String, private var _name: String, private var _slug: String,
                   ownerName: String, authorNames: List[String], homepage: Option[String],
                   private var recommendedVersionId: Option[Int],
                   private var categoryId: Int = -1, private var _views: Int,
                   private var _downloads: Int, private var _stars: Int,
                   private var _issues: Option[String], private var _source: Option[String]) extends Model {

  def this(pluginId: String, name: String, owner: String, authors: List[String], homepage: String) = {
    this(None, None, pluginId, sanitizeName(name), slugify(name),
         owner, authors, Option(homepage), None, 0, 0, 0, 0, None, None)
  }

  def owner: Dev = Dev(this.ownerName) // TODO: Teams

  def authors: List[Dev] = for (author <- authorNames) yield Dev(author) // TODO: Teams

  /**
    * Returns the name of this project.
    *
    * @return Name of project
    */
  def name: String = this._name

  /**
    * Returns this Project's URL slug.
    *
    * @return URL slug
    */
  def slug: String = this._slug

  /**
    * Sets the name of this project and performs all the necessary renames.
    *
    * @param _name   New name
    * @return       Future result
    */
  def name_=(_name: String) = {
    val newName = sanitizeName(_name)
    checkArgument(Project.isNamespaceAvailable(this.ownerName, newName), "slug not available", "")
    checkArgument(isValidName(newName), "invalid name", "")

    now(Queries.Projects.setString(this, _.name, newName)).get
    ProjectManager.renameProject(this.ownerName, this.name, newName)
    this._name = newName

    val newSlug = slugify(this)
    now(Queries.Projects.setString(this, _.slug, newSlug)).get
    this._slug = newSlug
  }

  /**
    * Returns all Channels belonging to this Project.
    *
    * @return All channels in project
    */
  def channels: Seq[Channel] = now(Queries.Channels.in(this.id.get)).get

  /**
    * Returns the Channel in this project with the specified name.
    *
    * @param name   Name of channel
    * @return       Channel with name, if present, None otherwise
    */
  def channel(name: String): Option[Channel] = now(Queries.Channels.withName(this.id.get, name)).get

  /**
    * Returns the Channel in this project with the specified color. Colors are
    * unique to Channels within Projects.
    *
    * @param color  Color of channel
    * @return       Channel with color, if present, None otherwise
    */
  def channel(color: ChannelColor): Option[Channel] = now(Queries.Channels.withColor(this.id.get, color.id)).get

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name   Name of channel
    * @return       New channel
    */
  def newChannel(name: String, color: ChannelColor): Try[Channel] = Try {
    checkArgument(Channel.isValidName(name), "invalid name", "")
    checkState(this.channels.size < Project.MAX_CHANNELS, "channel limit reached", "")
    now(Queries.Channels.create(new Channel(name, color, this.id.get))).get
  }

  /**
    * Returns this Project's recommended version.
    *
    * @return Recommended version
    */
  def recommendedVersion: Version = now(Queries.Versions.withId(this.recommendedVersionId.get)).get.get

  /**
    * Updates this project's recommended version.
    *
    * @param _version  Version to set
    * @return         Result
    */
  def recommendedVersion_=(_version: Version) = {
    now(Queries.Projects.setInt(this, _.recommendedVersionId, _version.id.get)).get
    this.recommendedVersionId = _version.id
  }

  /**
    * Returns all Versions belonging to this Project.
    *
    * @return All versions in project
    */
  def versions: Seq[Version] = now(Queries.Versions.inProject(this.id.get)).get

  /**
    * Returns all Versions belonging to the specified channels.
    *
    * @param channels   Channels to get versions for
    * @return           All versions in channels
    */
  def versions(channels: Seq[Channel]): Seq[Version] = {
    channels.foreach(c => checkArgument(c.projectId == this.id.get, "channel doesn't belong to project", ""))
    now(Queries.Versions.inChannels(channels.map(_.id.get))).get
  }

  /**
    * Returns this Project's category.
    *
    * @return Project category
    */
  def category: Category = Categories(this.categoryId)

  /**
    * Sets this Project's category.
    *
    * @param _category Category to set
    */
  def category_=(_category: Category) = {
    if (this.exists) {
      now(Queries.Projects.setInt(this, _.categoryId, _category.id)).get
    }
    this.categoryId = _category.id
  }

  /**
    * Returns the amount of unique views on this Project.
    *
    * @return Unique views on project
    */
  def views: Int = this._views

  /**
    * Increments this Project's view count by one.
    *
    * @return Future result
    */
  def addView() = {
    now(Queries.Projects.setInt(this, _.views, this._views + 1)).get
    this._views += 1
  }

  /**
    * Returns the amount of unique downloads this Project has.
    *
    * @return Amount of unique downloads
    */
  def downloads: Int = this._downloads

  /**
    * Increments this Project's downloadc count by one.
    *
    * @return Future result
    */
  def addDownload() = {
    now(Queries.Projects.setInt(this, _.downloads, this._downloads + 1)).get
    this._downloads += 1
  }

  /**
    * Returns the amount of times this Project has been starred.
    *
    * @return Amount of stars
    */
  def stars: Int = this._stars

  /**
    * Returns true if this Project is starred by the specified User.
    *
    * @param user   User to check if starred for
    * @return       True if starred by User
    */
  def isStarredBy(user: User): Boolean = now(Queries.Projects.isStarredBy(this.id.get, user.externalId)).get

  /**
    * Returns true if this Project is starred by the User with the specified
    * username.
    *
    * @param username   To get User of
    * @return           True if starred by User
    */
  def isStarredBy(username: String): Boolean = isStarredBy(User.withName(username).get)

  /**
    * Sets this Project as starred for the specified User.
    *
    * @param user   User to star for
    * @return       Future result
    */
  def starFor(user: User) = {
    now(Queries.Projects.starFor(this.id.get, user.externalId)).get
    this._stars += 1
  }

  /**
    * Removes a star for this Project for the specified User.
    *
    * @param user   User to unstar for
    * @return       Future result
    */
  def unstarFor(user: User) = {
    now(Queries.Projects.unstarFor(this.id.get, user.externalId)).get
    this._stars -= 1
  }

  /**
    * Returns the link to this Project's issue tracker, if any.
    *
    * @return Link to issue tracker
    */
  def issues: Option[String] = this._issues

  /**
    * Sets the link to this Project's issue tracker.
    *
    * @param _issues Issue tracker link
    */
  def issues_=(_issues: String) = {
    now(Queries.Projects.setString(this, _.issues, _issues))
    this._issues = Option(_issues)
  }

  /**
    * Returns the link to this Project's source code, if any.
    *
    * @return Link to source
    */
  def source: Option[String] = this._source

  /**
    * Sets the link to this Project's source code.
    *
    * @param _source Source code link
    */
  def source_=(_source: String) = {
    now(Queries.Projects.setString(this, _.source, _source))
    this._source = Option(_source)
  }

  /**
    * Returns this Project's pages.
    *
    * @return Project pages
    */
  def pages: Seq[Page] = now(Queries.Pages.in(this.id.get)).get

  /**
    * Returns the Page with the specified name, if any.
    *
    * @param name   Page name
    * @return       Page with name, if any, None otherwise
    */
  def page(name: String): Option[Page] = now(Queries.Pages.withName(this.id.get, name)).get

  /**
    * Returns true if a page with the specified name exists.
    *
    * @param name   Page name
    * @return       True if exists
    */
  def pageExists(name: String): Boolean = page(name).isDefined

  /**
    * Returns the specified Page or creates it if it doesn't exist.
    *
    * @param name   Page name
    * @return       Page with name or new name if it doesn't exist
    */
  def getOrCreatePage(name: String): Page = {
    now(Queries.Pages.getOrCreate(new Page(this.id.get, name, Page.template(name), true))).get
  }

  /**
    * Deletes the page with the specified name.
    *
    * @param name Page name
    */
  def deletePage(name: String) = {
    now(Queries.Pages.delete(page(name).get)).get
    Unit
  }

  /**
    * Returns this Project's home page.
    *
    * @return Project home page
    */
  def homePage: Page = {
    now(Queries.Pages.getOrCreate(new Page(this.id.get, Page.HOME, Page.template(name, Page.HOME_MESSAGE), false))).get
  }

  /**
    * Returns true if this Project already exists.
    *
    * @return True if project exists, false otherwise
    */
  def exists: Boolean = withName(this.ownerName, this.name).isDefined

  /**
    * Returns true if the Project's desired slug is available.
    *
    * @return True if slug is available
    */
  def isNamespaceAvailable: Boolean = Project.isNamespaceAvailable(this.ownerName, this._slug)

  /**
    * Immediately deletes this projects and any associated files.
    *
    * @return Result
    */
  def delete: Try[Unit] = Try {
    now(Queries.Projects.delete(this)).get
    FileUtils.deleteDirectory(ProjectManager.projectDir(this.ownerName, this._name).toFile)
  }

  /**
    * Returns a presentable date string of this version's creation date.
    *
    * @return Creation date string
    */
  def prettyDate: String = DATE_FORMAT.format(this.createdAt.get)

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Project] && o.asInstanceOf[Project].id.get == this.id.get
  }

}

object Project {

  /**
    * The maximum length for a Project name.
    */
  val MAX_NAME_LENGTH: Int = 25

  /**
    * The maximum amount of Pages a Project can have.
    */
  val MAX_PAGES: Int = 10

  /**
    * The maximum amount of Channels permitted in a single Project.
    */
  val MAX_CHANNELS = 5

  /**
    * The format used for displaying dates regarding Projects.
    */
  val DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy")

  /**
    * Represents a Project with an uploaded plugin that has not yet been
    * created.
    *
    * @param project        Pending project
    * @param firstVersion   Uploaded plugin
    */
  case class PendingProject(project: Project, firstVersion: PluginFile) extends PendingAction[Project] with Cacheable {

    private var pendingVersion: Option[PendingVersion] = None

    /**
      * Creates a new PendingVersion for this PendingProject
      *
      * @return New PendingVersion
      */
    def initFirstVersion: PendingVersion = {
      val meta = this.firstVersion.meta.get
      val version = Version.fromMeta(this.project, meta)
      val pending = Version.setPending(project.ownerName, project.slug,
        Channel.getSuggestedNameForVersion(version.versionString), version, this.firstVersion)
      this.pendingVersion = Some(pending)
      pending
    }

    /**
      * Returns this PendingProject's PendingVersion
      *
      * @return PendingVersion
      */
    def getPendingVersion: Option[PendingVersion] = this.pendingVersion

    override def complete: Try[Project] = Try {
      free()
      val newProject = ProjectManager.createProject(this).get
      val newVersion = ProjectManager.createVersion(this.pendingVersion.get).get
      newProject.recommendedVersion = newVersion
      newProject
    }

    override def cancel() = {
      free()
      this.firstVersion.delete()
      if (project.exists) {
        project.delete
      }
    }

    override def key: String = this.project.ownerName + '/' + this.project.slug

  }

  def withSlug(owner: String, slug: String): Option[Project] = now(Queries.Projects.withSlug(owner, slug)).get

  def withName(owner: String, name: String): Option[Project] = now(Queries.Projects.withName(owner, name)).get

  def withPluginId(pluginId: String): Option[Project] = now(Queries.Projects.withPluginId(pluginId)).get

  def withId(id: Int): Option[Project] = now(Queries.Projects.withId(id)).get

  def by(owner: String): Seq[Project] = now(Queries.Projects.by(owner)).get

  /**
    * Returns true if the Project's desired slug is available.
    *
    * @return True if slug is available
    */
  def isNamespaceAvailable(owner: String, slug: String): Boolean = {
    withSlug(owner, slug).isEmpty
  }

  /**
    * Returns true if the specified name is a valid Project name.
    *
    * @param name   Name to check
    * @return       True if valid name
    */
  def isValidName(name: String): Boolean = {
    val sanitized = sanitizeName(name)
    sanitized.length >= 1 && sanitized.length <= MAX_NAME_LENGTH
  }

  /**
    * Trims the specified name and removes extra whitespace.
    *
    * @param name   Name to sanitize
    * @return       Sanitized name
    */
  def sanitizeName(name: String): String = name.trim.replaceAll(" +", " ")

  /**
    * Returns a URL slug that should be used for the project.
    *
    * @return URL slug
    */
  def slugify(project: Project): String = slugify(project.name)

  /**
    * Returns a URL slug that should be used for the project.
    *
    * @return URL slug
    */
  def slugify(name: String) = sanitizeName(name).replace(' ', '-')

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project        Project that is pending
    * @param firstVersion   Uploaded plugin
    */
  def setPending(project: Project, firstVersion: PluginFile) =  {
    PendingProject(project, firstVersion).cache()
  }

  /**
    * Returns the PendingProject of the specified owner and name, if any.
    *
    * @param owner  Project owner
    * @param slug   Project slug
    * @return       PendingProject if present, None otherwise
    */
  def getPending(owner: String, slug: String): Option[PendingProject] = {
    Cache.getAs[PendingProject](owner + '/' + slug)
  }

  /**
    * Creates a new Project from the specified PluginMetadata.
    *
    * @param owner  Owner of project
    * @param meta   PluginMetadata object
    * @return       New project
    */
  def fromMeta(owner: String, meta: PluginMetadata): Project = {
    new Project(meta.getId, meta.getName, owner, meta.getAuthors.toList, meta.getUrl)
  }

}
