package models.user

import java.sql.Timestamp

import db.Model
import db.impl.ModelKeys._
import db.impl.OreModel
import db.meta.Bind
import ore.NotificationTypes.NotificationType
import ore.UserOwned

import scala.annotation.meta.field

/**
  * Represents a [[User]] notification.
  *
  * @param id               Unique ID
  * @param createdAt        Instant of cretion
  * @param userId           ID of User this notification belongs to
  * @param notificationType Type of notification
  * @param message          Message to display
  * @param action           Action to perform on click
  * @param read             True if notification has been read
  */
case class Notification(override val id: Option[Int] = None,
                        override val createdAt: Option[Timestamp] = None,
                        @(Bind @field) override val userId: Int,
                        @(Bind @field) originId: Int,
                        @(Bind @field) notificationType: NotificationType,
                        @(Bind @field) message: String,
                        @(Bind @field) action: Option[String] = None,
                        @(Bind @field) private var read: Boolean = false)
                        extends OreModel(id, createdAt)
                          with UserOwned {

  /**
    * Returns the [[User]] from which this Notification originated from.
    *
    * @return User from which this originated from
    */
  def origin: User = this.userBase.get(this.originId).get

  /**
    * Returns true if this notification has been read.
    *
    * @return True if read
    */
  def isRead: Boolean = this.read

  /**
    * Sets this notification as read or unread.
    *
    * @param read True if has been read
    */
  def setRead(read: Boolean) = Defined {
    this.read = read
    update(Read)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
