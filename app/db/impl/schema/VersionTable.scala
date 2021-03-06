package db.impl.schema
import java.sql.Timestamp

import db.DbRef
import db.impl.OrePostgresDriver.api._
import db.impl.table.common.{DescriptionColumn, DownloadsColumn, VisibilityColumn}
import db.table.ModelTable
import models.admin.VersionVisibilityChange
import models.project.{Channel, Project, ReviewState, Version}
import models.user.User

class VersionTable(tag: Tag)
    extends ModelTable[Version](tag, "project_versions")
    with DownloadsColumn[Version]
    with DescriptionColumn[Version]
    with VisibilityColumn[Version] {

  def versionString     = column[String]("version_string")
  def dependencies      = column[List[String]]("dependencies")
  def projectId         = column[DbRef[Project]]("project_id")
  def channelId         = column[DbRef[Channel]]("channel_id")
  def fileSize          = column[Long]("file_size")
  def hash              = column[String]("hash")
  def authorId          = column[DbRef[User]]("author_id")
  def reviewStatus      = column[ReviewState]("review_state")
  def reviewerId        = column[DbRef[User]]("reviewer_id")
  def approvedAt        = column[Timestamp]("approved_at")
  def fileName          = column[String]("file_name")
  def signatureFileName = column[String]("signature_file_name")

  override def * =
    (
      id.?,
      createdAt.?,
      (
        projectId,
        versionString,
        dependencies,
        channelId,
        fileSize,
        hash,
        authorId,
        description.?,
        downloads,
        reviewStatus,
        reviewerId.?,
        approvedAt.?,
        visibility,
        fileName,
        signatureFileName
      )
    ) <> (mkApply((Version.apply _).tupled), mkUnapply(Version.unapply))
}
