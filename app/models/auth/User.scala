package models.auth

import java.sql.Timestamp
import java.util.Date

case class User(externalId: Int, createdAt: Timestamp, name: String, username: String, email: String) {

  def this(externalId: Int, name: String, username: String, email: String) = {
    this(externalId, new Timestamp(new Date().getTime), name, username, email)
  }

}
