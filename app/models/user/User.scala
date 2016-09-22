package models.user

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.ModelKeys._
import db.impl._
import db.impl.access.{FlagBase, OrganizationBase, UserBase, VersionBase}
import db.impl.action.{ProjectActions, UserActions}
import db.impl.pg.OrePostgresDriver.api._
import db.meta._
import db.meta.relation.{ManyToMany, ManyToManyCollection, OneToMany}
import db.{ImmutableModelAccess, ModelAccess}
import models.project.{Flag, Project}
import models.user.role.{OrganizationRole, ProjectRole}
import ore.Visitable
import ore.permission._
import ore.permission.role.RoleTypes.{DonorType, RoleType}
import ore.permission.role._
import ore.permission.scope._
import ore.user.UserOwned
import play.api.mvc.Session
import util.StringUtils._

import scala.annotation.meta.field

/**
  * Represents a Sponge user.
  *
  * @param id           External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param _name        Full name of user
  * @param _username    Username
  * @param _email       Email
  * @param _tagline     The user configured "tagline" displayed on the user page.
  */
@ManyToManyCollection(Array(
  new ManyToMany(modelClass = classOf[Project], tableClass = classOf[ProjectWatchersTable]),
  new ManyToMany(modelClass = classOf[Organization], tableClass = classOf[OrganizationMembersTable])
))
@OneToMany(Array(
  classOf[Project], classOf[ProjectRole], classOf[OrganizationRole], classOf[Flag], classOf[Notification],
  classOf[Organization]
))
case class User(override val id: Option[Int] = None,
                override val createdAt: Option[Timestamp] = None,
                @(Bind @field) private var _name: Option[String] = None,
                @(Bind @field) private var _username: String,
                @(Bind @field) private var _email: Option[String] = None,
                @(Bind @field) private var _tagline: Option[String] = None,
                @(Bind @field) private var _globalRoles: List[RoleType] = List(),
                @(Bind @field) private var _joinDate: Option[Timestamp] = None,
                @(Bind @field) private var _avatarUrl: Option[String] = None)
                extends OreModel(id, createdAt)
                  with UserOwned
                  with ScopeSubject
                  with Visitable {

  override type M = User
  override type T = UserTable
  override type A = UserActions

  /**
    * The User's [[PermissionPredicate]]. All permission checks go through
    * here.
    */
  val can: PermissionPredicate = PermissionPredicate(this)
  val cannot: PermissionPredicate = PermissionPredicate(this, not = true)

  /**
    * The User's username
    *
    * @return Username
    */
  def username: String = this._username

  /**
    * Sets this User's username.
    *
    * @param _username Username of User
    */
  def username_=(_username: String) = {
    checkNotNull(_username, "username cannot be null", "")
    this._username = _username
    if (isDefined) update(Username)
  }

  /**
    * Returns this User's email.
    *
    * @return User email
    */
  def email: Option[String] = this._email

  /**
    * Sets this User's email.
    *
    * @param _email User email
    */
  def email_=(_email: String) = {
    this._email = Option(_email)
    if (isDefined) update(Email)
  }

  /**
    * Returns this User's full name.
    *
    * @return Full name of user
    */
  def fullName: Option[String] = this._name

  /**
    * Sets this User's full name.
    *
    * @param _fullName Full name of user
    */
  def fullName_=(_fullName: String) = {
    this._name = Option(_fullName)
    if (isDefined) update(Name)
  }

  /**
    * Returns the date this User joined any Sponge services.
    *
    * @return Join date
    */
  def joinDate: Option[Timestamp] = this._joinDate

  /**
    * Sets the Timestamp instant when this User joined Sponge for the first
    * time.
    *
    * @param _joinDate Sponge join date
    */
  def joinDate_=(_joinDate: Timestamp) = {
    this._joinDate = Option(_joinDate)
    if (isDefined) update(JoinDate)
  }

  /**
    * Returns a template string for constructing an avatar URL.
    *
    * @return Avatar template
    */
  def avatarTemplate: Option[String] = this._avatarUrl.map(this.config.forums.getString("baseUrl").get + _)

  /**
    * Returns a URL to this user's avatar for the specified size
    *
    * @param size avatar size
    * @return     avatar url
    */
  def avatarUrl(size: Int = 100): String = this.avatarTemplate.map { s =>
    s.replace("{size}", size.toString)
  }.getOrElse("")

  /**
    * Returns this User's avatar url.
    *
    * @return Avatar url
    */
  def avatarUrl: Option[String] = this._avatarUrl

  /**
    * Sets this User's avatar url.
    *
    * @param _avatarUrl Avatar url
    */
  def avatarUrl_=(_avatarUrl: String) = {
    this._avatarUrl = Option(_avatarUrl)
    if (isDefined) update(AvatarUrl)
  }

  /**
    * Returns this user's tagline.
    *
    * @return User tagline
    */
  def tagline: Option[String] = this._tagline

  /**
    * Sets this User's "tagline" that is displayed on the User page.
    *
    * @param _tagline Tagline to display
    */
  def tagline_=(_tagline: String) = {
    checkArgument(_tagline.length <= config.users.getInt("max-tagline-len").get, "tagline too long", "")
    this._tagline = Option(nullIfEmpty(_tagline))
    if (isDefined) update(Tagline)
  }

  /**
    * Returns this user's global [[RoleType]]s.
    *
    * @return Global RoleTypes
    */
  def globalRoles: Set[RoleType] = this._globalRoles.toSet

  /**
    * Sets the [[RoleTypes]]s that this User has globally.
    *
    * @param _globalRoles Roles to set
    */
  def globalRoles_=(_globalRoles: Set[RoleType]) = {
    this._globalRoles = _globalRoles.toList
    if (isDefined) update(GlobalRoles)
  }

  /**
    * Returns the highest level [[DonorType]] this User has.
    *
    * @return Highest level donor type
    */
  def donorType: Option[DonorType] = {
    this.globalRoles.toList
      .filter(_.isInstanceOf[DonorType])
      .map(_.asInstanceOf[DonorType])
      .sortBy(_.id).lastOption
  }

  /**
    * Returns this User's highest level of Trust.
    *
    * @return Highest level of trust
    */
  def trustIn(scope: Scope = GlobalScope): Trust = Defined {
    println(scope)
    scope match {
      case GlobalScope =>
        this.globalRoles.map(_.trust).toList.sorted.lastOption.getOrElse(Default)
      case pScope: ProjectScope =>
        pScope.project.memberships.members
          .find(_.user.equals(this))
          .flatMap(_.roles.filter(_.isAccepted).toList.sorted.lastOption.map(_.roleType.trust))
          .getOrElse(Default)
      case oScope: OrganizationScope =>
        oScope.organization.memberships.members
          .find(_.user.equals(this))
          .flatMap(_.roles.filter(_.isAccepted).toList.sorted.lastOption.map(_.roleType.trust))
          .getOrElse(Default)
      case _ => throw new RuntimeException("unknown scope: " + scope)
    }
  }

  /**
    * Returns the Projects that this User has starred.
    *
    * @param page Page of user stars
    * @return     Projects user has starred
    */
  def starred(page: Int = -1): Seq[Project] = Defined {
    val starsPerPage = this.config.users.getInt("stars-per-page").get
    val limit = if (page < 1) -1 else starsPerPage
    val actions = this.service.getActions(classOf[ProjectActions])
    this.service.await(actions.starredBy(this.id.get, limit, (page - 1) * starsPerPage)).get
  }

  /**
    * Returns true if this User is the currently authenticated user.
    *
    * @return True if currently authenticated user
    */
  def isCurrent(implicit session: Session): Boolean = this.service.access(classOf[UserBase]).current.exists { user =>
    user.equals(this) || (this.isOrganization && this.toOrganization.owner.user.equals(user))
  }

  /**
    * Fills the mutable field in this User with the specified User's
    * non-missing mutable fields.
    *
    * @param user User to fill with
    * @return     This user
    */
  def fill(user: User): User = {
    if (user == null) return this
    user.fullName.foreach(this.fullName_=)
    user.email.foreach(this.email_=)
    user.tagline.foreach(this.tagline_=)
    user.joinDate.foreach(this.joinDate_=)
    user._avatarUrl.foreach(this.avatarUrl_=)
    this.username = user.username
    this.globalRoles = user.globalRoles
    this
  }

  /**
    * Pulls information from the forums and updates this User.
    *
    * @return This user
    */
  def refresh(): User = {
    this.service.await(this.forums.fetchUser(this.name)).get.foreach(fill)
    this
  }

  /**
    * Returns all [[Project]]s owned by this user.
    *
    * @return Projects owned by user
    */
  def projects = ImmutableModelAccess(this.oneToMany[ProjectTable, Project](classOf[Project]))

  /**
    * Returns the Project with the specified name that this User owns.
    *
    * @param name   Name of project
    * @return       Owned project, if any, None otherwise
    */
  def getProject(name: String): Option[Project] = this.projects.find(equalsIgnoreCase(_.name, name))

  /**
    * Returns a [[ModelAccess]] of [[ProjectRole]]s.
    *
    * @return ProjectRoles
    */
  def projectRoles = ImmutableModelAccess(this.oneToMany[ProjectRoleTable, ProjectRole](classOf[ProjectRole]))

  /**
    * Returns the [[Organization]]s that this User owns.
    *
    * @return Organizations user owns
    */
  def ownedOrganizations = ImmutableModelAccess(this.oneToMany[OrganizationTable, Organization](classOf[Organization]))

  /**
    * Returns the [[Organization]]s that this User belongs to.
    *
    * @return Organizations user belongs to
    */
  def organizations = {
    this.manyToMany[OrganizationMembersTable, OrganizationTable, Organization](classOf[Organization],
      classOf[OrganizationMembersTable])
  }

  /**
    * Returns a [[ModelAccess]] of [[OrganizationRole]]s.
    *
    * @return OrganizationRoles
    */
  def organizationRoles
  = ImmutableModelAccess(this.oneToMany[OrganizationRoleTable, OrganizationRole](classOf[OrganizationRole]))

  /**
    * Returns true if this User is also an organization.
    *
    * @return True if organization
    */
  def isOrganization: Boolean = this.service.access(classOf[OrganizationBase]).exists(_.id === this.id.get)

  /**
    * Converts this User to an [[Organization]].
    *
    * @return Organization
    */
  def toOrganization: Organization = {
    this.service.access(classOf[OrganizationBase]).get(this.id.get)
      .getOrElse(throw new IllegalStateException("user is not an organization"))
  }

  /**
    * Returns the [[Project]]s that this User is watching.
    *
    * @return Projects user is watching
    */
  def watching
  = this.manyToMany[ProjectWatchersTable, ProjectTable, Project](classOf[Project], classOf[ProjectWatchersTable])

  /**
    * Sets the "watching" status on the specified project.
    *
    * @param project Project to update status on
    * @param watching True if watching
    */
  def setWatching(project: Project, watching: Boolean) = {
    val assoc = this.actions.getAssociation(classOf[ProjectWatchersTable])
    if (watching)
      assoc.assoc(this, project)
    else
      assoc.disassoc(this, project)
  }

  /**
    * Returns the [[Flag]]s submitted by this User.
    *
    * @return Flags submitted by user
    */
  def flags = ImmutableModelAccess(this.oneToMany[FlagTable, Flag](classOf[Flag]))

  /**
    * Returns true if the User has an unresolved [[Flag]] on the specified
    * [[Project]].
    *
    * @param project  Project to check
    * @return         True if has pending flag on Project
    */
  def hasUnresolvedFlagFor(project: Project): Boolean
  = this.flags.exists(f => f.projectId === project.id.get && !f.isResolved)

  /**
    * Returns this User's notifications.
    *
    * @return User notifications
    */
  def notifications = ImmutableModelAccess(this.oneToMany[NotificationTable, Notification](classOf[Notification]))

  /**
    * Sends a [[Notification]] to this user.
    *
    * @param notification Notification to send
    * @return Future result
    */
  def sendNotification(notification: Notification) = {
    this.config.debug("Sending notification: " + notification, -1)
    this.service.access[NotificationTable, Notification](classOf[Notification])
      .add(notification.copy(userId = this.id.get))
  }

  /**
    * Returns true if this User has any unread notifications.
    *
    * @return True if has unread notifications
    */
  def hasUnreadNotifications: Boolean = {
    ((this can ReviewFlags in GlobalScope) && this.service.access(classOf[FlagBase]).unresolved.nonEmpty) ||
      ((this can ReviewProjects in GlobalScope) && this.service.access(classOf[VersionBase]).notReviewed.nonEmpty) ||
      this.notifications.filterNot(_.read).nonEmpty
  }

  override val name = this.username
  override val url = this.username
  override val scope = GlobalScope
  override def userId = this.id.get
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(createdAt = theTime)

}
