package models.auth

import java.sql.Timestamp
import java.util.Date

import db.query.Queries
import db.Model
import Queries.now

/**
  * Represents a Sponge user.
  *
  * @param externalId   External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param name         Full name of user
  * @param username     Username
  * @param email        Email
  */
case class User(externalId: Int, createdAt: Timestamp, name: String,
                username: String, email: String) extends Model {

  def this(externalId: Int, name: String, username: String, email: String) = {
    this(externalId, new Timestamp(new Date().getTime), name, username, email)
  }

  override def id: Option[Int] = Some(this.externalId)

}

object User {

  /**
    * Returns the user with the specified username.
    *
    * @param name Username to find user for
    * @return User if found, None otherwise
    */
  def withName(name: String): Option[User] = now(Queries.Users.withName(name)).get

}
