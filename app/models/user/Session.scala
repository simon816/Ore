package models.user

import java.sql.Timestamp
import java.util.Date

import db.Model
import db.impl.SessionTable
import db.impl.access.UserBase
import db.impl.model.OreModel

/**
  * Represents a persistant [[User]] session.
  *
  * @param id         Unique ID
  * @param createdAt  Instant of creation
  * @param expiration Instant of expiration
  * @param username   Username session belongs to
  * @param token      Unique token
  */
case class Session(override val id: Option[Int] = None,
                   override val createdAt: Option[Timestamp] = None,
                   expiration: Timestamp,
                   username: String,
                   token: String) extends OreModel(id, createdAt) {

  override type M = Session
  override type T = SessionTable

  /**
    * Returns true if this Session has expired.
    *
    * @return
    */
  def hasExpired: Boolean = expiration.before(new Date)

  /**
    * Returns the [[User]] that this Session belongs to.
    *
    * @param users UserBase instance
    * @return User session belongs to
    */
  def user(implicit users: UserBase) = users.withName(this.username).get

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
