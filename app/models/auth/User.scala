package models.auth

import java.sql.Timestamp
import java.util.Date

/**
  * Represents a Sponge user.
  *
  * @param externalId   External ID provided by authentication.
  * @param createdAt    Date this user first logged onto Ore.
  * @param name         Full name of user
  * @param username     Username
  * @param email        Email
  */
case class User(externalId: Int, createdAt: Timestamp, name: String, username: String, email: String) {

  def this(externalId: Int, name: String, username: String, email: String) = {
    this(externalId, new Timestamp(new Date().getTime), name, username, email)
  }

}
