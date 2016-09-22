package ore.user.notification

/**
  * A collection of ways to filter notifications.
  */
object NotificationFilters extends Enumeration {

  val Unread = NotificationFilter(0, "unread", "notification.empty.unread")
  val Read = NotificationFilter(1, "read", "notification.empty.read")
  val All = NotificationFilter(2, "all", "notification.empty.all")

  case class NotificationFilter(i: Int, name: String, emptyMessage: String) extends super.Val(i, name)
  implicit def convert(value: Value): NotificationFilter = value.asInstanceOf[NotificationFilter]

}
