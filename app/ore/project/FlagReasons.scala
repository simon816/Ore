package ore.project

import db.impl.OrePostgresDriver
import db.table.MappedType
import slick.jdbc.JdbcType

import scala.language.implicitConversions

/**
  * Represents the reasons for submitting a [[models.project.Flag]].
  */
object FlagReasons extends Enumeration {

  val InappropriateContent = FlagReason(0, "Inappropriate Content")
  val Impersonation        = FlagReason(1, "Impersonation or Deception")
  val Spam                 = FlagReason(2, "Spam")
  val MalIntent            = FlagReason(3, "Malicious Intent")
  val Other                = FlagReason(4, "Other")

  case class FlagReason(i: Int, title: String) extends super.Val(i, title) with MappedType[FlagReason] {
    implicit val mapper: JdbcType[FlagReason] = OrePostgresDriver.api.flagReasonTypeMapper
  }
  implicit def convert(value: Value): FlagReason = value.asInstanceOf[FlagReason]

}
