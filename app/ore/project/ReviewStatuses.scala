package ore.project

import db.impl.OrePostgresDriver
import db.table.MappedType
import ore.Colors
import ore.Colors.Color
import slick.jdbc.JdbcType

import scala.language.implicitConversions

object ReviewStatuses extends Enumeration {

  val Unassigned = ReviewStatus(0, "admin.queue.unassigned", Colors.Gray)
  val Assigned = ReviewStatus(1, "admin.queue.assigned", Colors.Blue)
  val InProgress = ReviewStatus(2, "admin.queue.inProgress", Colors.Gold)
  val Accepted = ReviewStatus(3, "admin.queue.accepted", Colors.Green)
  val Rejected = ReviewStatus(4, "admin.queue.rejected", Colors.Red)

  case class ReviewStatus(i: Int, title: String, color: Color)
    extends super.Val(i, title) with MappedType[ReviewStatus] {
    implicit val mapper: JdbcType[ReviewStatus] = OrePostgresDriver.api.reviewStatusTypeMapper
  }
  implicit def convert(value: Value): ReviewStatus = value.asInstanceOf[ReviewStatus]

}
