package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.ModelKeys._
import db.impl.OrePostgresDriver.api._
import db.impl._
import db.impl.schema.ProjectSchema
import models.statistic.ProjectView
import models.user.User
import models.user.role.ProjectRole
import ore.Colors.Color
import ore.permission.scope.ProjectScope
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason
import ore.project.{Categories, ProjectMember}
import ore.user.MembershipDossier
import ore.{Joinable, OreEnv, Visitable}
import util.StringUtils
import util.StringUtils.{compact, slugify}

/**
  * Represents an Ore package.
  *
  * <p>Note: As a general rule, do not handle actions / results in model classes</p>
  *
  * @param id                     Unique identifier
  * @param createdAt              Instant of creation
  * @param pluginId               Plugin ID
  * @param _name                  Name of plugin
  * @param _ownerName             The owner Author for this project
  * @param homepage               The external project URL
  * @param recommendedVersionId   The ID of this project's recommended version
  * @param _category              The project's Category
  * @param _downloads             How many times this project has been downloaded in total
  * @param _issues                External link to issue tracker
  * @param _source                External link to source code
  * @param _description           Short description of Project
  */
case class Project(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                   pluginId: String,
                   private var _ownerName: String,
                   private var _ownerId: Int,
                   homepage: Option[String] = None,
                   private var _name: String,
                   private var _slug: String,
                   private var recommendedVersionId: Option[Int] = None,
                   private var _category: Category = Categories.Undefined,
                   private var _downloads: Int = 0,
                   private var _issues: Option[String] = None,
                   private var _source: Option[String] = None,
                   private var _description: Option[String] = None,
                   private var _topicId: Option[Int] = None,
                   private var _postId: Option[Int] = None,
                   private var _isVisible: Boolean = true,
                   private var _lastUpdated: Timestamp = null)
                   extends OreModel(id, createdAt)
                     with ProjectScope
                     with Visitable
                     with Joinable[ProjectMember] {

  override type M = Project
  override type T = ProjectTable
  override type S = ProjectSchema

  /**
    * Contains all information for [[User]] memberships.
    */
  override val memberships = new MembershipDossier {

    type ModelType = Project
    type RoleType = ProjectRole
    type RoleTable = ProjectRoleTable
    type MemberType = ProjectMember
    type MembersTable = ProjectMembersTable

    val membersTableClass: Class[MembersTable] = classOf[ProjectMembersTable]
    val roleClass: Class[RoleType] = classOf[ProjectRole]
    val model: ModelType = Project.this

    def newMember(userId: Int): MemberType = new ProjectMember(this.model, userId)

  }

  def this(pluginId: String, name: String, owner: String, ownerId: Int, homepage: String) = {
    this(pluginId=pluginId, _name=compact(name), _slug=slugify(name),
         _ownerName=owner, _ownerId=ownerId, homepage=Option(homepage))
  }

  /**
    * Returns the ID of the [[User]] that owns this Project.
    *
    * @return ID of user
    */
  def ownerId: Int = this._ownerId

  /**
    * Returns the name of the [[User]] that owns this Project.
    *
    * @return Name of user that owns project
    */
  def ownerName: String = this._ownerName

  /**
    * Returns the owner [[ProjectMember]] of this project.
    *
    * @return Owner Member of project
    */
  override def owner: ProjectMember = new ProjectMember(this, this.ownerId)

  /**
    * Sets the [[User]] that owns this Project.
    *
    * @param user User that owns project
    */
  def owner_=(user: User) = {
    this._ownerId = user.id.get
    this._ownerName = user.name
    if (isDefined) {
      update(OwnerId)
      update(OwnerName)
    }
  }

  /**
    * Returns ModelAccess to the user's who are watching this project.
    *
    * @return Users watching project
    */
  def watchers = this.schema.getAssociation[ProjectWatchersTable, User](classOf[ProjectWatchersTable], this)

  /**
    * Returns the name of this Project.
    *
    * @return Name of project
    */
  override def name: String = this._name

  /**
    * Sets the name of this project.
    *
    * @param _name New name
    */
  def name_=(_name: String) = Defined {
    this._name = _name
    update(Name)
  }

  /**
    * Returns this Project's URL slug.
    *
    * @return URL slug
    */
  def slug: String = this._slug

  /**
    * Sets the URL slug of this project.
    *
    * @param _slug New slug
    */
  def slug_=(_slug: String) = Defined {
    this._slug = _slug
    update(Slug)
  }

  /**
    * Returns the base URL for this Project.
    *
    * @return Base URL for project
    */
  override def url: String = this.ownerName + '/' + this.slug

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
    * Returns the last time this [[Project]] was updated.
    *
    * @return Last time project was updated
    */
  def lastUpdated: Timestamp = this._lastUpdated

  /**
    * Sets the last time this [[Project]] was updated.
    *
    * @param lastUpdated Last time project was updated
    */
  def lastUpdated_=(lastUpdated: Timestamp) = {
    this._lastUpdated = lastUpdated
    if (isDefined) update(LastUpdated)
  }

  /**
    * Returns the record of unique Project views.
    *
    * @return Unique project views
    */
  def views = this.schema.getChildren[ProjectView](classOf[ProjectView], this)

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
    * Returns [[db.access.ModelAccess]] to [[User]]s who have starred this
    * project.
    *
    * @return Users who have starred this project
    */
  def stars = Defined(this.schema.getAssociation[ProjectStarsTable, User](classOf[ProjectStarsTable], this))

  /**
    * Sets the "starred" state of this Project for the specified User.
    *
    * @param user User to set starred state of
    * @param starred True if should star
    */
  def setStarredBy(user: User, starred: Boolean) = {
    if (starred)
      this.stars.add(user)
    else
      this.stars.remove(user)
  }

  /**
    * Returns all flags on this project.
    *
    * @return Flags on project
    */
  def flags = this.schema.getChildren[Flag](classOf[Flag], this)

  /**
    * Submits a flag on this project for the specified user.
    *
    * @param user   Flagger
    * @param reason Reason for flagging
    */
  def flagFor(user: User, reason: FlagReason) = Defined {
    val userId = user.id.get
    checkArgument(userId != this.ownerId, "cannot flag own project", "")
    this.service.access[Flag](classOf[Flag]).add(new Flag(this.id.get, user.id.get, reason))
  }

  /**
    * Returns the Channels in this Project.
    *
    * @return Channels in project
    */
  def channels = this.schema.getChildren[Channel](classOf[Channel], this)

  /**
    * Creates a new Channel for this project with the specified name.
    *
    * TODO: Move elsewhere
    *
    * @param name   Name of channel
    * @return       New channel
    */
  def addChannel(name: String, color: Color): Channel = Defined {
    checkArgument(this.config.isValidChannelName(name), "invalid name", "")
    checkState(this.channels.size < this.config.projects.getInt("max-channels").get, "channel limit reached", "")
    this.service.access[Channel](classOf[Channel]).add(new Channel(name, color, this.id.get))
  }

  /**
    * Returns all versions in this project.
    *
    * @return Versions in project
    */
  def versions = this.schema.getChildren[Version](classOf[Version], this)

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
  def pages = this.schema.getChildren[Page](classOf[Page], this)

  /**
    * Returns this Project's home page.
    *
    * @return Project home page
    */
  def homePage: Page = Defined {
    val page = new Page(this.id.get, Page.HomeName, Page.Template(this.name, Page.HomeMessage), false)
    this.service.await(page.schema.getOrInsert(page)).get
  }

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
    this.service.await(page.schema.getOrInsert(page)).get
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
    * Returns the string to fill the specified Project's forum topic content
    * with.
    *
    * @return Topic content string
    */
  def topicContent(implicit env: OreEnv): String = {
    val templatePath = env.conf.resolve("discourse/project_topic.md")
    StringUtils.readAndFormatFile(templatePath, this.name, this.url, this.homePage.contents)
  }

  override def projectId = Defined(this.id.get)
  override def copyWith(id: Option[Int], theTime: Option[Timestamp])
  = this.copy(id = id, createdAt = theTime, _lastUpdated = theTime.orNull)
  override def hashCode() = this.id.get.hashCode
  override def equals(o: Any) = o.isInstanceOf[Project] && o.asInstanceOf[Project].id.get == this.id.get

}
