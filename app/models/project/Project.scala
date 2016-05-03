package models.project

import java.nio.file.{Files, Path}
import java.sql.Timestamp
import java.text.MessageFormat

import ore.Colors.Color
import ore.permission.scope.ProjectScope
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason
import ore.project.util._
import ore.project.{Categories, ProjectMember}
import com.google.common.base.Preconditions._
import db._
import db.dao.ModelSet
import db.driver.OrePostgresDriver.api._
import db.model.ModelKeys._
import db.model._
import db.model.annotation.{Bind, BindingsGenerator, HasMany}
import db.query.ModelQueries
import db.query.ModelQueries.{await, filterToFunction}
import forums.SpongeForums
import models.project.Version.PendingVersion
import models.user.{ProjectRole, User}
import org.apache.commons.io.FileUtils
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.Play.current
import play.api.cache.Cache
import util.C._
import util.P._
import util.StringUtils.{compact, slugify}
import util.{Cacheable, PendingAction}

import scala.annotation.meta.field
import scala.util.Try

/**
  * Represents an Ore package.
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * @param id                     Unique identifier
  * @param createdAt              Instant of creation
  * @param pluginId               Plugin ID
  * @param _name                  Name of plugin
  * @param ownerName              The owner Author for this project
  * @param homepage               The external project URL
  * @param recommendedVersionId   The ID of this project's recommended version
  * @param _category              The project's Category
  * @param _views                 How many times this project has been views
  * @param _downloads             How many times this project has been downloaded in total
  * @param _stars                 How many times this project has been starred
  * @param _issues                External link to issue tracker
  * @param _source                External link to source code
  * @param _description           Short description of Project
  */
@HasMany(Array(classOf[Channel], classOf[Version], classOf[Page], classOf[Flag], classOf[ProjectRole]))
case class Project(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                                pluginId: String,
                                ownerName: String,
                                ownerId: Int,
                                homepage: Option[String] = None,
                   @(Bind @field) private var _name: String,
                   @(Bind @field) private var _slug: String,
                   @(Bind @field) private var recommendedVersionId: Option[Int] = None,
                   @(Bind @field) private var _category: Category = Categories.Undefined,
                   @(Bind @field) private var _views: Int = 0,
                   @(Bind @field) private var _downloads: Int = 0,
                   @(Bind @field) private var _stars: Int = 0,
                   @(Bind @field) private var _issues: Option[String] = None,
                   @(Bind @field) private var _source: Option[String] = None,
                   @(Bind @field) private var _description: Option[String] = None,
                   @(Bind @field) private var _topicId: Option[Int] = None,
                   @(Bind @field) private var _postId: Option[Int] = None,
                   @(Bind @field) private var _isVisible: Boolean = true,
                   @(Bind @field) private var _isReviewed: Boolean = false)
                   extends Model(id, createdAt) with ProjectScope { self =>

  import models.project.Project._

  override type M <: Project { type M = self.M }

  BindingsGenerator.generateFor(this)

  def this(pluginId: String, name: String, owner: String, ownerId: Int, homepage: String) = {
    this(pluginId=pluginId, _name=compact(name), _slug=slugify(name),
         ownerName=owner, ownerId=ownerId, homepage=Option(homepage))
  }

  /**
    * Returns the owner [[ProjectMember]] of this project.
    *
    * @return Owner Member of project
    */
  def owner: ProjectMember = new ProjectMember(this, this.ownerName)

  /**
    * Returns all [[ProjectMember]]s of this project.
    *
    * @return All Members of project
    */
  def members: List[ProjectMember] = await(ModelQueries.Projects.getMembers(this)).get

  /**
    * Removes the [[ProjectMember]] that belongs to the specified [[User]] from this
    * project.
    *
    * @param user User to remove
    */
  def removeMember(user: User) = this.roles.removeAll(_.userId === user.id.get)

  /**
    * Returns the name of this Project.
    *
    * @return Name of project
    */
  def name: String = this._name

  /**
    * Sets the name of this project and performs all the necessary renames.
    *
    * @param _name   New name
    * @return       Future result
    */
  def name_=(_name: String) = Defined {
    val newName = compact(_name)
    val newSlug = slugify(newName)
    checkArgument(isValidName(newName), "invalid name", "")
    checkArgument(Project.isNamespaceAvailable(this.ownerName, newSlug), "slug not available", "")
    ProjectFiles.renameProject(this.ownerName, this.name, newName)
    this._name = newName
    this._slug = slugify(newName)
    if (this.topicId.isDefined) {
      SpongeForums.Embed.renameTopic(this)
      SpongeForums.Embed.updateTopic(this)
    }
    update(Name)
    update(Slug)
  }

  /**
    * Returns this Project's URL slug.
    *
    * @return URL slug
    */
  def slug: String = this._slug

  /**
    * Returns this Project's description.
    *
    * @return Project description
    */
  def description: Option[String] = this._description

  /**
    * Sets this Project's description.
    *
    * @param _description Description to set
    */
  def description_=(_description: String) = {
    checkArgument(_description == null || _description.length <= MaxDescriptionLength, "description too long", "")
    this._description = Option(_description)
    if (this.topicId.isDefined) SpongeForums.Embed.renameTopic(this)
    if (isDefined) update(Description)
  }

  /**
    * Returns this Project's category.
    *
    * @return Project category
    */
  def category: Category = this._category

  /**
    * Sets this Project's category.
    *
    * @param _category Category to set
    */
  def category_=(_category: Category) = {
    this._category = _category
    if (isDefined) update(ModelKeys.Category)
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
    this._views += 1
    if (isDefined) update(Views)
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
    this._downloads += 1
    if (isDefined) update(Downloads)
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
  def isStarredBy(user: User): Boolean = Defined {
    await(ModelQueries.Projects.isStarredBy(this.id.get, user.id.get)).get
  }

  /**
    * Returns true if this Project is starred by the User with the specified
    * username.
    *
    * @param username   To get User of
    * @return           True if starred by User
    */
  def isStarredBy(username: String): Boolean = Defined {
    val user = User.withName(username)
    isStarredBy(user.get)
  }

  /**
    * Sets the "starred" state of this Project for the specified User.
    *
    * @param user User to set starred state of
    * @param starred True if should star
    */
  def setStarredBy(user: User, starred: Boolean) = {
    if (starred) starFor(user) else unstarFor(user)
  }

  /**
    * Sets this Project as starred for the specified User.
    *
    * @param user   User to star for
    * @return       Future result
    */
  def starFor(user: User) = Defined {
    if (!isStarredBy(user)) {
      this._stars += 1
      await(ModelQueries.Projects.starFor(this.id.get, user.id.get)).get
      update(Stars)
    }
  }

  /**
    * Removes a star for this Project for the specified User.
    *
    * @param user   User to unstar for
    * @return       Future result
    */
  def unstarFor(user: User) = Defined {
    if (isStarredBy(user)) {
      this._stars -= 1
      await(ModelQueries.Projects.unstarFor(this.id.get, user.id.get)).get
      update(Stars)
    }
  }

  /**
    * Submits a flag on this project for the specified user.
    *
    * @param user   Flagger
    * @param reason Reason for flagging
    */
  def flagFor(user: User, reason: FlagReason) = Defined {
    val userId = user.id.get
    checkArgument(userId != this.ownerId, "cannot flag own project", "")
    await(ModelQueries insert new Flag(this.id.get, user.id.get, reason)).get
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
    this._issues = Option(_issues)
    if (isDefined) update(Issues)
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
    this._source = Option(_source)
    if (isDefined) update(Source)
  }

  /**
    * Returns the forum topic ID for this Project.
    *
    * @return Forum topic ID
    */
  def topicId: Option[Int] = this._topicId

  /**
    * Sets the forum topic ID for this Project.
    *
    * @param _topicId ID to set
    */
  def topicId_=(_topicId: Int) = Defined {
    this._topicId = Some(_topicId)
    update(TopicId)
  }

  /**
    * Returns the forum post ID for this Project.
    *
    * @return Forum post ID
    */
  def postId: Option[Int] = this._postId

  /**
    * Sets the forum post ID for this Project.
    *
    * @param _postId Forum post ID
    */
  def postId_=(_postId: Int) = Defined {
    this._postId = Some(_postId)
    update(PostId)
  }

  /**
    * Returns a [[ModelSet]] of all [[ProjectRole]]s in this Project.
    *
    * @return Set of all ProjectRoles
    */
  def roles = this.getMany[ProjectRoleTable, ProjectRole](classOf[ProjectRole])

  /**
    * Returns the Channels in this Project.
    *
    * @return Channels in project
    */
  def channels = this.getMany[ChannelTable, Channel](classOf[Channel])

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name   Name of channel
    * @return       New channel
    */
  def addChannel(name: String, color: Color): Channel = Defined {
    checkArgument(Channel.isValidName(name), "invalid name", "")
    checkState(this.channels.size < Project.MaxChannels, "channel limit reached", "")
    await(ModelQueries insert new Channel(name, color, this.id.get)).get
  }

  /**
    * Returns all versions in this project.
    *
    * @return Versions in project
    */
  def versions = this.getMany[VersionTable, Version](classOf[Version])

  /**
    * Returns this Project's recommended version.
    *
    * @return Recommended version
    */
  def recommendedVersion: Version = this.versions.withId(this.recommendedVersionId.get).get

  /**
    * Updates this project's recommended version.
    *
    * @param _version  Version to set
    * @return         Result
    */
  def recommendedVersion_=(_version: Version) = {
    this.recommendedVersionId = _version.id
    if (isDefined) update(RecommendedVersionId)
  }

  /**
    * Returns the pages in this Project.
    *
    * @return Pages in project
    */
  def pages = this.getMany[PageTable, Page](classOf[Page])

  /**
    * Returns true if a page with the specified name exists.
    *
    * @param name   Page name
    * @return       True if exists
    */
  def pageExists(name: String): Boolean = this.pages.exists(_.name === name)

  /**
    * Returns the specified Page or creates it if it doesn't exist.
    *
    * @param name   Page name
    * @return       Page with name or new name if it doesn't exist
    */
  def getOrCreatePage(name: String): Page = Defined {
    await(ModelQueries.Pages.getOrInsert(new Page(this.id.get, name, Page.Template(name, Page.HomeMessage), true))).get
  }

  /**
    * Returns this Project's home page.
    *
    * @return Project home page
    */
  def homePage: Page = Defined {
    val page = new Page(this.id.get, Page.HomeName, Page.Template(this.name, Page.HomeMessage), false)
    await(ModelQueries.Pages.getOrInsert(page)).get
  }

  /**
    * Returns true if this Project is visible.
    *
    * @return True if visible
    */
  def isVisible: Boolean = this._isVisible

  /**
    * Sets whether this project is visible.
    *
    * @param visible True if visible
    */
  def setVisible(visible: Boolean) = {
    this._isVisible = visible
    if (isDefined) update(IsVisible)
  }

  /**
    * Returns true if this Project has been reviewed by the moderation staff.
    *
    * @return True if reviewed
    */
  def isReviewed: Boolean = this._isReviewed

  /**
    * Sets whether this project is under review.
    *
    * @param reviewed True if reviewed by staff
    */
  def setReviewed(reviewed: Boolean) = {
    this._isReviewed = reviewed
    if (isDefined) update(IsReviewed)
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
  def delete = Defined {
    await(ModelQueries delete this).get
    FileUtils.deleteDirectory(ProjectFiles.projectDir(this.ownerName, this._name).toFile)
    if (this.topicId.isDefined) SpongeForums.Embed.deleteTopic(this)
  }

  override def projectId = Defined(this.id.get)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Project = this.copy(id = id, createdAt = theTime)

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Project] && o.asInstanceOf[Project].id.get == this.id.get
  }

}

object Project extends ModelSet[ProjectTable, Project](classOf[Project]) {

  /**
    * The maximum length for a Project name.
    */
  val MaxNameLength: Int = ProjectsConf.getInt("max-name-len").get

  /**
    * The maximum length for a Project description.
    */
  val MaxDescriptionLength: Int = ProjectsConf.getInt("max-desc-len").get

  /**
    * The maximum amount of Pages a Project can have.
    */
  val MaxPages: Int = ProjectsConf.getInt("max-pages").get

  /**
    * The maximum amount of Channels permitted in a single Project.
    */
  val MaxChannels = ProjectsConf.getInt("max-channels").get

  /**
    * The maximum amount of Projects that are loaded initially.
    */
  val InitialLoad: Int = ProjectsConf.getInt("init-load").get

  /**
    * The path to the template file to build a forum topic from.
    */
  val ForumTopicTemplatePath: Path = ConfDir.resolve("discourse/project_topic.md")

  /**
    * Returns the Project with the specified owner name and URL slug, if any.
    *
    * @param owner  Owner name
    * @param slug   URL slug
    * @return       Project if found, None otherwise
    */
  def withSlug(owner: String, slug: String): Option[Project]
  = this.find(ModelQueries.Projects.ownerFilter(owner) && (_.slug === slug))

  /**
    * Returns the Project with the specified owner name and Project name, if
    * any.
    *
    * @param owner  Owner name
    * @param name   Project name
    * @return       Project if found, None otherwise
    */
  def withName(owner: String, name: String): Option[Project]
  = this.find(ModelQueries.Projects.ownerFilter(owner) && (_.name.toLowerCase === name.toLowerCase))

  /**
    * Returns the Project with the specified plugin ID, if any.
    *
    * @param pluginId Plugin ID
    * @return         Project if found, None otherwise
    */
  def withPluginId(pluginId: String): Option[Project] = this.find(_.pluginId === pluginId)

  /**
    * Returns the string to fill the specified Project's forum topic content
    * with.
    *
    * @param project  Project of topic
    * @return         Topic content string
    */
  def topicContentFor(project: Project): String = {
    val template = new String(Files.readAllBytes(ForumTopicTemplatePath))
    val url = AppConf.getString("baseUrl").get + '/' + project.ownerName + '/' + project.slug
    MessageFormat.format(template, project.name, url, project.homePage.contents)
  }

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
    val sanitized = compact(name)
    sanitized.length >= 1 && sanitized.length <= MaxNameLength
  }

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project        Project that is pending
    * @param firstVersion   Uploaded plugin
    */
  def setPending(project: Project, firstVersion: PluginFile): PendingProject =  {
    val pendingProject = PendingProject(project, firstVersion)
    pendingProject.cache()
    pendingProject
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
  def fromMeta(owner: User, meta: PluginMetadata): Project = {
    new Project(meta.getId, meta.getName, owner.username, owner.id.get, meta.getUrl)
  }

  /**
    * Represents a Project with an uploaded plugin that has not yet been
    * created.
    *
    * @param project  Pending project
    * @param file     Uploaded plugin
    */
  case class PendingProject(project: Project,
                            file: PluginFile,
                            var roles: Set[ProjectRole] = Set())
    extends PendingAction[Project]
      with Cacheable {

    /**
      * The first [[PendingVersion]] for this PendingProject.
      */
    val pendingVersion: PendingVersion = {
      val version = Version.fromMeta(this.project, this.file)
      Version.setPending(project.ownerName, project.slug,
        Channel.getSuggestedNameForVersion(version.versionString), version, this.file)
    }

    override def complete: Try[Project] = Try {
      free()
      val newProject = ProjectFactory.createProject(this).get
      val newVersion = ProjectFactory.createVersion(this.pendingVersion).get
      newProject.recommendedVersion = newVersion
      newProject
    }

    override def cancel() = {
      free()
      this.file.delete()
      if (project.isDefined) {
        project.delete
      }
    }

    override def key: String = this.project.ownerName + '/' + this.project.slug

  }

}
