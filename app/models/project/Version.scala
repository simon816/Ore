package models.project

import java.sql.Timestamp

import sql.Storage

/**
  * Represents a single version of a Project.
  *
  * @param id             Unique identifier
  * @param createdAt      Instant of creation
  * @param channelId      ID of channel this version belongs to
  * @param versionString  Version string
  */
case class Version(id: Int, createdAt: Timestamp, projectId: Int, channelId: Int, versionString: String) {

  def this(projectId: Int, channelId: Int, versionString: String) = this(-1, null, projectId, channelId, versionString)

  def getProject: Project = Storage.getProject(this.projectId).get

  def getChannel: Channel = Storage.getChannel(this.channelId).get

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Version] && o.asInstanceOf[Version].id == this.id

}
