package models.user

import java.sql.Timestamp

import db.Model
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
                        @(Bind @field) notificationType: NotificationType,
                        @(Bind @field) message: String,
                        @(Bind @field) action: String,
                        @(Bind @field) read: Boolean = false)
                        extends OreModel(id, createdAt)
                          with UserOwned {

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
