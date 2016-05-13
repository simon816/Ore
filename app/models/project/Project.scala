package models.project

import java.nio.file.Files
import java.sql.Timestamp
import java.text.MessageFormat

import com.google.common.base.Preconditions._
import db.action.ModelAccess
import db.impl.ModelKeys._
import db.impl.OrePostgresDriver.api._
import db.impl._
import db.impl.action.ProjectActions
import db.meta.{Actor, Bind, HasMany}
import forums.DiscourseApi
import models.statistic.ProjectView
import models.user.{ProjectRole, User}
import ore.Colors.Color
import ore.permission.scope.ProjectScope
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason
import ore.project.util._
import ore.project.{Categories, ProjectMember}
import util.StringUtils.{compact, slugify}
import util.{OreConfig, OreEnv}

import scala.annotation.meta.field

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
  * @param _downloads             How many times this project has been downloaded in total
  * @param _stars                 How many times this project has been starred
  * @param _issues                External link to issue tracker
  * @param _source                External link to source code
  * @param _description           Short description of Project
  */
@Actor(classOf[ProjectActions])
@HasMany(Array(
  classOf[Channel], classOf[Version], classOf[Page],
  classOf[Flag], classOf[ProjectRole], classOf[ProjectView]
))
case class Project(// Immutable
                   override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                                pluginId: String,
                                ownerName: String,
                                ownerId: Int,
                                homepage: Option[String] = None,
                   // Mutable
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
                   @(Bind @field) private var _isVisible: Boolean = true)
                   extends OreModel(id, createdAt)
                     with ProjectScope { self =>

  override type M = Project
  override type T = ProjectTable
  override type A = ProjectActions

  import models.project.Project._

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
  def members: Set[ProjectMember] = service.await(this.actions.getMembers(this)).get.toSet

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
  def name_=(_name: String)(implicit fileManager: ProjectFileManager) = Defined {
    val newName = compact(_name)
    val newSlug = slugify(newName)
    checkArgument(isValidName(newName), "invalid name", "")
    checkArgument(this.projectBase.isNamespaceAvailable(this.ownerName, newSlug), "slug not available", "")
    fileManager.renameProject(this.ownerName, this.name, newName)
    this._name = newName
    this._slug = slugify(newName)
    if (this.topicId.isDefined) {
      this.forums.embed.renameTopic(this)
      this.forums.embed.updateTopic(this)
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
    checkArgument(_description == null
      || _description.length <= this.config.projects.getInt("max-desc-len").get, "description too long", "")
    this._description = Option(_description)
    if (this.topicId.isDefined) this.forums.embed.renameTopic(this)
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
    * Returns the record of unique Project views.
    *
    * @return Unique project views
    */
  def viewEntries = this.getRelated[ProjectViewsTable, ProjectView](classOf[ProjectView])

  /**
    * Returns the amount of unique views this Project has.
    *
    * @return Amount of unique views
    */
  def views: Int = this._views

  /**
    * Adds one view to this Project's view count.
    */
  def addView() = {
    this._views += 1
    update(Views)
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
    service.await(this.actions.isStarredBy(this.id.get, user.id.get)).get
  }

  /**
    * Returns true if this Project is starred by the User with the specified
    * username.
    *
    * @param username   To get User of
    * @return           True if starred by User
    */
  def isStarredBy(username: String): Boolean = Defined {
    val user = this.userBase.withName(username)
    isStarredBy(user.get)
  }

  /**
    * Sets the "starred" state of this Project for the specified User.
    *
    * @param user User to set starred state of
    * @param starred True if should star
    */
  def setStarredBy(user: User, starred: Boolean) = if (starred) starFor(user) else unstarFor(user)

  /**
    * Sets this Project as starred for the specified User.
    *
    * @param user   User to star for
    * @return       Future result
    */
  def starFor(user: User) = Defined {
    if (!isStarredBy(user)) {
      this._stars += 1
      service.await(this.actions.starFor(this.id.get, user.id.get)).get
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
      service.await(this.actions.unstarFor(this.id.get, user.id.get)).get
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
    this.flags.add(new Flag(this.id.get, user.id.get, reason))
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
    * Returns a [[ModelAccess]] of all [[ProjectRole]]s in this Project.
    *
    * @return Set of all ProjectRoles
    */
  def roles = this.getRelated[ProjectRoleTable, ProjectRole](classOf[ProjectRole])

  /**
    * Returns the Channels in this Project.
    *
    * @return Channels in project
    */
  def channels = this.getRelated[ChannelTable, Channel](classOf[Channel])

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * @param name   Name of channel
    * @return       New channel
    */
  def addChannel(name: String, color: Color): Channel = Defined {
    checkArgument(Channel.isValidName(name), "invalid name", "")
    checkState(this.channels.size < config.projects.getInt("max-channels").get, "channel limit reached", "")
    this.channels.add(new Channel(name, color, this.id.get))
  }

  /**
    * Returns all versions in this project.
    *
    * @return Versions in project
    */
  def versions = this.getRelated[VersionTable, Version](classOf[Version])

  /**
    * Returns this Project's recommended version.
    *
    * @return Recommended version
    */
  def recommendedVersion: Version = this.versions.get(this.recommendedVersionId.get).get

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
  def pages = this.getRelated[PageTable, Page](classOf[Page])

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
    val page = new Page(this.id.get, name, Page.Template(name, Page.HomeMessage), true)
    service.await(page.actions.getOrInsert(page)).get
  }

  /**
    * Returns all flags on this project.
    *
    * @return Flags on project
    */
  def flags = this.getRelated[FlagTable, Flag](classOf[Flag])

  /**
    * Returns this Project's home page.
    *
    * @return Project home page
    */
  def homePage: Page = Defined {
    val page = new Page(this.id.get, Page.HomeName, Page.Template(this.name, Page.HomeMessage), false)
    service.await(page.actions.getOrInsert(page)).get
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
    * Returns the string to fill the specified Project's forum topic content
    * with.
    *
    * @return Topic content string
    */
  def topicContent(implicit env: OreEnv): String = {
    val template = new String(Files.readAllBytes(env.conf.resolve("discourse/project_topic.md")))
    val url = config.app.getString("baseUrl").get + '/' + project.ownerName + '/' + project.slug
    MessageFormat.format(template, project.name, url, project.homePage.contents)
  }

  override def projectId = Defined(this.id.get)
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)
  override def hashCode() = this.id.get.hashCode
  override def equals(o: Any) = o.isInstanceOf[Project] && o.asInstanceOf[Project].id.get == this.id.get

}

object Project {

  /**
    * Returns true if the specified name is a valid Project name.
    *
    * @param name   Name to check
    * @return       True if valid name
    */
  def isValidName(name: String)(implicit config: OreConfig): Boolean = {
    val sanitized = compact(name)
    sanitized.length >= 1 && sanitized.length <= config.projects.getInt("max-name-len").get
  }

}
