package ore.project

object FlagReasons extends Enumeration {

  val InappropriateContent = FlagReason(0, "Inappropriate Content")
  val Dmca                 = FlagReason(1, "DMCA Takedown Request")
  val Impersonation        = FlagReason(2, "Impersonation or Deception")
  val Spam                 = FlagReason(3, "Spam")
  val MalIntent            = FlagReason(4, "Malicious Intent")

  case class FlagReason(i: Int, title: String) extends super.Val(i, title)
  implicit def convert(value: Value): FlagReason = value.asInstanceOf[FlagReason]

}
