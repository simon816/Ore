package models.user

import java.sql.Timestamp

import db.Model
import db.query.Queries
import db.query.Queries.now
import models.project.Project
import ore.UserRoles
import ore.UserRoles.UserRole

/**
  * Represents a Sponge user.
  *
  * @param externalId   External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param name         Full name of user
  * @param username     Username
  * @param email        Email
  */
case class User(externalId: Int, override val createdAt: Option[Timestamp], name: Option[String],
                username: String, email: String, roleIds: List[Int] = List()) extends Model {

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
    * @return Projects user has starred
    */
  def starred: Seq[Project] = now(Queries.Projects.starredBy(this.externalId)).get

  override def id: Option[Int] = Some(this.externalId)

}

object User {

  /**
    * Returns the user with the specified username.
    *
    * @param username Username of user
    * @return User if found, None otherwise
    */
  def withName(username: String): Option[User] = now(Queries.Users.withName(username)).get

}
