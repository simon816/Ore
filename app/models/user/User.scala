package models.user

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.dao.ModelSet
import db.driver.OrePostgresDriver.api._
import db.model.Model
import db.model.ModelKeys._
import db.model.annotation.{Bind, BindingsGenerator}
import db.query.ModelQueries
import db.query.ModelQueries.await
import db.{FlagTable, ProjectRoleTable, ProjectTable, UserTable}
import forums.SpongeForums
import models.project.{Flag, Project}
import ore.UserOwner
import ore.permission._
import ore.permission.role.RoleTypes.RoleType
import ore.permission.role._
import ore.permission.scope.{GlobalScope, ProjectScope, Scope, ScopeSubject}
import play.api.mvc.Session
import util.C._
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
case class User(override val  id: Option[Int] = None,
                override val  createdAt: Option[Timestamp] = None,
                @(Bind @field) private var _name: Option[String] = None,
                @(Bind @field) private var _username: String,
                @(Bind @field) private var _email: Option[String] = None,
                @(Bind @field) private var _tagline: Option[String] = None,
                @(Bind @field) private var _globalRoles: List[RoleType] = List(),
                @(Bind @field) private var _joinDate: Option[Timestamp] = None,
                @(Bind @field) private var _avatarUrl: Option[String] = None)
                extends Model(id, createdAt) with UserOwner with ScopeSubject { self =>

  import models.user.User._

  BindingsGenerator.generateFor(this)

  bindChild[ProjectTable, Project](classOf[Project], _.ownerId)
  bindChild[ProjectRoleTable, ProjectRole](classOf[ProjectRole], _.userId)
  bindChild[FlagTable, Flag](classOf[Flag], _.userId)

  override type M <: User { type M = self.M }

  /**
    * The User's [[PermissionPredicate]]. All permission checks go through
    * here.
    */
  val can: PermissionPredicate = PermissionPredicate(this)
  val cannot: PermissionPredicate = PermissionPredicate(this, not = true)

  private val forumsUrl = DiscourseConf.getString("baseUrl").get

  /**
    * Returns this User's full name.
    *
    * @return Full name of user
    */
  def name: Option[String] = this._name

  /**
    * Sets this User's full name.
    *
    * @param _fullName Full name of user
    */
  def name_=(_fullName: String) = {
    this._name = Option(_fullName)
    if (isDefined) update(Name)
  }

  /**
    * Returns this User's username.
    *
    * @return Username of User
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
    * Returns the Timestamp instant when this User joined Sponge for the first
    * time.
    *
    * @return Sponge join date
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
    * Returns this User's avatar url with the specified size.
    *
    * @param size Size of avatar
    * @return     Avatar URL
    */
  def avatarUrl(size: Int = 100): String = {
    this._avatarUrl.map(s => this.forumsUrl + s.replace("{size}", size.toString)).getOrElse("")
  }

  def avatarTemplate: Option[String] = this._avatarUrl.map(this.forumsUrl + _)

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
    * Returns this User's "tagline" that is displayed on the User page.
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
    checkArgument(_tagline.length <= MaxTaglineLength, "tagline too long", "")
    this._tagline = Option(nullIfEmpty(_tagline))
    if (isDefined) update(Tagline)
  }

  /**
    * Returns the Project with the specified name that this User owns.
    *
    * @param name   Name of project
    * @return       Owned project, if any, None otherwise
    */
  def getProject(name: String): Option[Project] = Project.withName(this.username, name)

  /**
    * Returns all Projects owned by this User.
    *
    * @return All projects owned by User
    */
  def projects = this.getChildren[ProjectTable, Project](classOf[Project])

  /**
    * Returns a [[ModelSet]] of [[ProjectRole]]s.
    *
    * @return ProjectRoles
    */
  def projectRoles = this.getChildren[ProjectRoleTable, ProjectRole](classOf[ProjectRole])

  /**
    * Returns a Set of [[RoleType]]s that this User has globally.
    *
    * @return Global RoleTypes.
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
    * Returns this User's highest level of Trust.
    *
    * @return Highest level of trust
    */
  def trustIn(scope: Scope = GlobalScope): Trust = Defined {
    scope match {
      case GlobalScope => this.globalRoles.map(_.trust).toList.sorted.reverse.headOption.getOrElse(Default)
      case pScope: ProjectScope =>
        this.projectRoles.find(_.projectId === pScope.projectId).map(_.roleType.trust).getOrElse(Default)
    }
  }

  /**
    * Returns the [[Flag]]s submitted by this User.
    *
    * @return Flags submitted by user
    */
  def flags = this.getChildren[FlagTable, Flag](classOf[Flag])

  /**
    * Returns true if the User has an unresolved [[Flag]] on the specified
    * [[Project]].
    *
    * @param project  Project to check
    * @return         True if has pending flag on Project
    */
  def hasUnresolvedFlagFor(project: Project): Boolean
  = this.flags.find(f => f.projectId === project.id.get && !f.isResolved).isDefined

  /**
    * Returns the Projects that this User has starred.
    *
    * @param page Page of user stars
    * @return     Projects user has starred
    */
  def starred(page: Int = -1): Seq[Project] = Defined {
    val limit = if (page < 1) -1 else StarsPerPage
    await(ModelQueries.Projects.starredBy(this.id.get, limit, (page - 1) * StarsPerPage)).get
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
    user.name.foreach(this.name_=)
    user.email.foreach(this.email_=)
    user.tagline.foreach(this.tagline_=)
    user.joinDate.foreach(this.joinDate_=)
    user._avatarUrl.foreach(this.avatarUrl_=)
    this.username = user.username
    this.globalRoles = user.globalRoles
    this
  }

  override val scope: Scope = GlobalScope

  override def userId = this.id.get

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): User = this.copy(createdAt = theTime)

}

object User extends ModelSet[UserTable, User](classOf[User]) {

  /**
    * The amount of stars displayed in the stars panel per page.
    */
  val StarsPerPage: Int = UsersConf.getInt("stars-per-page").get

  /**
    * The maximum length for User taglines.
    */
  val MaxTaglineLength: Int = UsersConf.getInt("max-tagline-len").get

  /**
    * Returns the user with the specified username.
    *
    * @param username Username of user
    * @return User if found, None otherwise
    */
  def withName(username: String): Option[User] = {
    this.find(_.username.toLowerCase === username.toLowerCase).orElse {
      await(SpongeForums.Users.fetch(username)).get.map(getOrCreate)
    }
  }

  /**
    * Attempts to find the specified User in the database or creates a new User
    * if one does not exist.
    *
    * @param user User to find
    * @return     Found or new User
    */
  def getOrCreate(user: User): User = await(ModelQueries.Users.getOrInsert(user)).get

  /**
    * Returns the currently authenticated User.
    *
    * @param session  Current session
    * @return         Authenticated user, if any, None otherwise
    */
  def current(implicit session: Session): Option[User] = session.get("username").map(withName).getOrElse(None)

}
