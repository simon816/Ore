package models.user

import java.sql.Timestamp

import com.google.common.base.Preconditions
import com.google.common.base.Preconditions._
import db.Model
import db.query.Queries
import db.query.Queries.now
import models.user.User._
import models.project.Project
import ore.UserRoles
import ore.UserRoles.UserRole
import play.api.Play.current
import play.api.Play.{configuration => config}

/**
  * Represents a Sponge user.
  *
  * @param externalId   External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param name         Full name of user
  * @param username     Username
  * @param email        Email
  * @param roleIds      List of roles this User is in
  * @param _tagline     The user configured "tagline" displayed on the user page.
  */
case class User(externalId: Int, override val createdAt: Option[Timestamp], name: Option[String],
                username: String, email: String, roleIds: List[Int] = List(),
                private var _tagline: Option[String] = None)
                extends Model {

  def this(externalId: Int, name: String, username: String, email: String) = {
    this(externalId, None, Option(name), username, email)
  }

  /**
    * Returns the set of UserRoles this User is in.
    *
    * @return UserRoles user has
    */
  def roles: Set[UserRole] = (for (roleId <- roleIds) yield {
    UserRoles(roleId).asInstanceOf[UserRole]
  }).toSet

  /**
    * Returns the Projects that this User has starred.
    *
    * @param page Page of user stars
    * @return     Projects user has starred
    */
  def starred(page: Int = -1): Seq[Project] = {
    val limit = if (page < 1) -1 else STARS_PER_PAGE
    now(Queries.Projects.starredBy(this.externalId, limit, (page - 1) * STARS_PER_PAGE)).get
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
    checkArgument(_tagline.length <= MAX_TAGLINE_LENGTH, "tagline too long", "")
    val tag = if (_tagline.nonEmpty) _tagline else null
    now(Queries.Users.setString(this, _.tagline, tag)).get
    this._tagline = Option(tag)
  }

  override def id: Option[Int] = Some(this.externalId)

}

object User {

  /**
    * The amount of stars displayed in the stars panel per page.
    */
  val STARS_PER_PAGE: Int = config.getInt("ore.users.stars-per-page").get

  /**
    * The maximum length for User taglines.
    */
  val MAX_TAGLINE_LENGTH: Int = config.getInt("ore.users.max-tagline-len").get

  /**
    * Returns the user with the specified username.
    *
    * @param username Username of user
    * @return User if found, None otherwise
    */
  def withName(username: String): Option[User] = now(Queries.Users.withName(username)).get

}
