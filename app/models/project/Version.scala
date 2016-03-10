package models.project

import java.sql.Timestamp

import sql.Storage

import scala.concurrent.Future

/**
  * Represents a single version of a Project.
  *
  * @param id             Unique identifier
  * @param createdAt      Instant of creation
  * @param channelId      ID of channel this version belongs to
  * @param versionString  Version string
  */
case class Version(id: Option[Int], var createdAt: Option[Timestamp], projectId: Int, channelId: Int, versionString: String) {

  def this(projectId: Int, channelId: Int, versionString: String) = this(None, None, projectId, channelId, versionString)

  /**
    * Returns the project this version belongs to.
    *
    * @return Project
    */
  def getProject: Future[Project] = Storage.getProject(this.projectId)

  /**
    * Returns the channel this version belongs to.
    *
    * @return Channel
    */
  def getChannel: Future[Channel] = Storage.getChannel(this.channelId)

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Version] && o.asInstanceOf[Version].id.get == this.id.get

}
