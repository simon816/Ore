package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.VersionReviewTable
import db.impl.model.OreModel
import db.impl.table.ModelKeys._
import models.user.User
import ore.project.{ProjectOwned, ReviewStatuses}
import ore.project.ReviewStatuses.ReviewStatus
import ore.user.UserOwned

case class VersionReview(override val id: Option[Int] = None,
                         override val createdAt: Option[Timestamp] = None,
                         versionId: Int,
                         override val projectId: Int,
                         private var _assigneeId: Int = -1,
                         private var _status: ReviewStatus = ReviewStatuses.Unassigned)
                         extends OreModel(id, createdAt) with UserOwned with ProjectOwned {

  override type M = VersionReview
  override type T = VersionReviewTable

  def version: Version = this.service.access[Version](classOf[Version]).get(this.versionId).get

  def assigneeId: Int = this._assigneeId

  def assignee: Option[User] = if (this.assigneeId == -1) None else this.userBase.get(this._assigneeId)

  def assignee_=(assignee: User) = {
    checkNotNull(assignee, "null user", "")
    checkArgument(assignee.id.isDefined, "undefined user", "")
    this._assigneeId = assignee.id.get
    if (isDefined) update(AssigneeId)
  }

  def status: ReviewStatus = this._status

  def status_=(status: ReviewStatus) = {
    checkNotNull(status, "null status", "")
    this._status = status
    if (isDefined) update(Status)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)
  override def userId = this._assigneeId

}
