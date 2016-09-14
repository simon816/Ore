package models.project

import java.sql.Timestamp

import db.Model
import db.impl.OreModel
import models.user.User
import ore.{ProjectOwned, UserOwned}

/**
  * Represents an invitation to join a [[Project]] as a [[ore.project.ProjectMember]].
  *
  * @param id         Unique ID
  * @param createdAt  Instant of creation
  * @param projectId  Project ID
  * @param userId     User ID
  */
case class ProjectInvite(override val id: Option[Int] = None,
                         override val createdAt: Option[Timestamp] = None,
                         override val projectId: Int,
                         override val userId: Int)
                         extends OreModel(id, createdAt)
                           with ProjectOwned
                           with UserOwned {

  def this(project: Project, user: User) = this(projectId = project.id.get, userId = user.id.get)

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(id = id, createdAt = theTime)

}
