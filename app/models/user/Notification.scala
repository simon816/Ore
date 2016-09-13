package models.user

import java.sql.Timestamp

import db.Model
import db.impl.OreModel
import db.meta.Bind
import ore.NotificationTypes.NotificationType
import ore.UserOwned

import scala.annotation.meta.field

case class Notification(override val id: Option[Int],
                        override val createdAt: Option[Timestamp],
                        @(Bind @field) override val userId: Int,
                        @(Bind @field) notificationType: NotificationType,
                        @(Bind @field) message: String,
                        @(Bind @field) action: String,
                        @(Bind @field) read: Boolean = false)
                        extends OreModel(id, createdAt)
                          with UserOwned {

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
