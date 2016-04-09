package models.auth

import java.sql.Timestamp

import db.Model
import db.query.Queries
import db.query.Queries.now

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
                username: String, email: String) extends Model {

  def this(externalId: Int, name: String, username: String, email: String) = {
    this(externalId, None, Option(name), username, email)
  }

  override def id: Option[Int] = Some(this.externalId)

}

object User {

  /**
    * Returns the user with the specified username.
    *
    * @return User if found, None otherwise
    */
  def withName(username: String): Option[User] = now(Queries.Users.withName(username)).get

}
