package models.user

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.OrePostgresDriver.api._
import db.orm.dao.{ModelDAO, ModelSet}
import db.orm.model.ModelKeys._
import db.orm.model.{Model, UserOwner}
import db.query.Queries
import db.query.Queries.now
import db.{FlagTable, ProjectRolesTable, UserTable}
import forums.SpongeForums
import models.project.{Flag, Project}
import ore.permission._
import ore.permission.role.RoleTypes.RoleType
import ore.permission.role._
import ore.permission.scope.{GlobalScope, ProjectScope, Scope, ScopeSubject}
import play.api.mvc.Session
import util.C._
import util.StringUtils._

/**
  * Represents a Sponge user.
  *
  * @param id           External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param _fullName    Full name of user
  * @param _username    Username
  * @param _email       Email
  * @param _tagline     The user configured "tagline" displayed on the user page.
  */
case class User(override val  id: Option[Int] = None,
                override val  createdAt: Option[Timestamp] = None,
                private var   _fullName: Option[String] = None,
                private var   _username: String,
                private var   _email: Option[String] = None,
                private var   _tagline: Option[String] = None,
                private var   globalRoleIds: List[Int] = List(),
                private var   _joinDate: Option[Timestamp] = None)
                extends       Model
                with          UserOwner
                with          ScopeSubject { self =>

  import models.user.User._

  override type M <: User { type M = self.M }

  /**
    * The User's [[PermissionPredicate]]. All permission checks go through
    * here.
    */
  val can: PermissionPredicate = PermissionPredicate(this)
  val cannot: PermissionPredicate = PermissionPredicate(this, not = true)

  def this(externalId: Int, name: String, username: String, email: String, joinDate: Timestamp) = {
    this(id=Some(externalId), _fullName=Option(name), _username=username,
         _email=Option(email), _joinDate=Option(joinDate))
  }

  def fullName: Option[String] = this._fullName

  def fullName_=(_fullName: String) = {
    this._fullName = Option(_fullName)
    if (isDefined) update(FullName)
  }

  def username: String = this._username

  def username_=(_username: String) = {
    checkNotNull(_username, "username cannot be null", "")
    this._username = _username
    if (isDefined) update(Username)
  }

  def email: Option[String] = this._email

  def email_=(_email: String) = {
    this._email = Option(_email)
    if (isDefined) update(Email)
  }

  def joinDate: Option[Timestamp] = this._joinDate

  def joinDate_=(_joinDate: Timestamp) = {
    this._joinDate = Option(_joinDate)
    if (isDefined) update(JoinDate)
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

  def fill(user: User): User = {
    if (user == null) return this
    user.fullName.map(this.fullName = _)
    user.email.map(this.email = _)
    user.tagline.map(this.tagline = _)
    user.joinDate.map(this.joinDate = _)
    this.username = user.username
    this.globalRoleTypes = user.globalRoleTypes
    this
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
  def projects: Seq[Project] = Project.by(this.username)

  /**
    * Returns a [[ModelSet]] of [[ProjectRole]]s.
    *
    * @return ProjectRoles
    */
  def projectRoles: ModelSet[UserTable, User, ProjectRolesTable, ProjectRole]
  = assertDefined(Queries.Users.getProjectRoles(this))

  /**
    * Returns a Set of [[RoleType]]s that this User has globally.
    *
    * @return Global RoleTypes.
    */
  def globalRoleTypes: Set[RoleType] = (for (roleId <- globalRoleIds) yield RoleTypes.withId(roleId)).toSet

  /**
    * Sets the [[RoleTypes]]s that this User has globally.
    *
    * @param _roles Roles to set
    */
  def globalRoleTypes_=(_roles: Set[RoleType]) = {
    this.globalRoleIds = _roles.map(_.roleId).toList
    if (isDefined) update(GlobalRoles)
  }

  /**
    * Returns this User's highest level of Trust.
    *
    * @return Highest level of trust
    */
  def trustIn(scope: Scope = GlobalScope): Trust = assertDefined {
    scope match {
      case GlobalScope => this.globalRoleTypes.map(_.trust).toList.sorted.reverse.headOption.getOrElse(Default)
      case pScope: ProjectScope =>
        this.projectRoles.find(_.projectId === pScope.projectId).map(_.roleType.trust).getOrElse(Default)
    }
  }

  /**
    * Returns the [[Flag]]s submitted by this User.
    *
    * @return Flags submitted by user
    */
  def flags: ModelSet[UserTable, User, FlagTable, Flag] = Queries.Users.getFlags(this)

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
  def starred(page: Int = -1): Seq[Project] = assertDefined {
    val limit = if (page < 1) -1 else StarsPerPage
    now(Queries.Projects.starredBy(this.id.get, limit, (page - 1) * StarsPerPage)).get
  }

  override val scope: Scope = GlobalScope

  override def userId = this.id.get

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): User = this.copy(createdAt = theTime)

  // Table bindings

  bind[String](FullName, _._fullName.orNull, fullName => Seq(Queries.Users.setString(this, _.name, fullName)))
  bind[String](Username, _._username, username => Seq(Queries.Users.setString(this, _.username, username)))
  bind[String](Email, _._email.orNull, email => Seq(Queries.Users.setString(this, _.email, email)))
  bind[String](Tagline, _._tagline.orNull, tagline => Seq(Queries.Users.setString(this, _.tagline, tagline)))
  bind[Timestamp](JoinDate, _._joinDate.orNull, joinDate =>
    Seq(Queries.Users.setTimestamp(this,  _.joinDate, joinDate)))
  bind[List[Int]](GlobalRoles, _.globalRoleIds, globalRoles =>
    Seq(Queries.Users.setIntList(this, _.globalRoles, globalRoles)))

}

object User extends ModelDAO[User] {

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
    now(Queries.Users.find(_.username.toLowerCase === username.toLowerCase)).get.orElse {
      now(SpongeForums.Users.fetch(username)).get.map(getOrCreate)
    }
  }

  def getOrCreate(user: User): User = now(Queries.Users.getOrInsert(user)).get

  /**
    * Returns the currently authenticated User.
    *
    * @param session  Current session
    * @return         Authenticated user, if any, None otherwise
    */
  def current(implicit session: Session): Option[User] = session.get("username").map(withName).getOrElse(None)

  override def withId(id: Int): Option[User] = now(Queries.Users.get(id)).get

}
