package ore.notification

/**
  * A collection of ways to filter invites.
  */
object InviteFilters extends Enumeration {

  val All = InviteFilter(0, "all")
  val Projects = InviteFilter(1, "projects")
  val Organizations = InviteFilter(2, "organizations")

  case class InviteFilter(i: Int, name: String) extends super.Val(i, name)
  implicit def convert(value: Value): InviteFilter = value.asInstanceOf[InviteFilter]

}
