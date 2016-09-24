package ore.user.notification

import db.MappedType
import db.impl.OrePostgresDriver
import slick.jdbc.JdbcType

/**
  * Represents the different types of notifications.
  */
object NotificationTypes extends Enumeration {

  val ProjectInvite = NotificationType(0)
  val OrganizationInvite = NotificationType(1)
  val NewProjectVersion = NotificationType(2)

  case class NotificationType(i: Int) extends super.Val(i) with MappedType[NotificationType] {
    implicit val mapper: JdbcType[NotificationType] = OrePostgresDriver.api.notificationTypeTypeMapper
  }
  implicit def convert(value: Value): NotificationType = value.asInstanceOf[NotificationType]

}
