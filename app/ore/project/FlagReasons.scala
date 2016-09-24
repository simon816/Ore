package ore.project

import db.MappedType
import db.impl.OrePostgresDriver
import slick.jdbc.JdbcType

/**
  * Represents the reasons for submitting a [[models.project.Flag]].
  */
object FlagReasons extends Enumeration {

  val InappropriateContent = FlagReason(0, "Inappropriate Content")
  val Dmca                 = FlagReason(1, "DMCA Takedown Request")
  val Impersonation        = FlagReason(2, "Impersonation or Deception")
  val Spam                 = FlagReason(3, "Spam")
  val MalIntent            = FlagReason(4, "Malicious Intent")

  case class FlagReason(i: Int, title: String) extends super.Val(i, title) with MappedType[FlagReason] {
    implicit val mapper: JdbcType[FlagReason] = OrePostgresDriver.api.flagReasonTypeMapper
  }
  implicit def convert(value: Value): FlagReason = value.asInstanceOf[FlagReason]

}
