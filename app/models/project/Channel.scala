package models.project

import java.sql.Timestamp

import models.project.Channel._
import sql.Storage

import scala.util.Try

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * TODO: Max channels per-project
  *
  * @param id         Unique identifier
  * @param createdAt  Instant of creation
  * @param projectId  ID of project this channel belongs to
  * @param name       Name of channel
  * @param colorHex   Hex color
  */
case class Channel(id: Int, createdAt: Timestamp, projectId: Int, name: String, colorHex: String) {

  def this(projectId: Int, name: String) = this(-1, null, projectId, name, HEX_GREEN)

  def getProject: Project = Storage.getProject(projectId).get

  def getVersions: Seq[Version] = Storage.getVersions(this.id)

  /**
    * Returns the Version in this channel with the specified version string.
    *
    * @param version Version string
    * @return Version, if any, None otherwise
    */
  def getVersion(version: String): Option[Version] = Storage.getVersion(this.id, version)

  /**
    * Creates a new version within this Channel.
    *
    * @param version Version string
    * @return New channel
    */
  def newVersion(version: String): Try[Version] = {
    Storage.createVersion(new Version(this.projectId, this.id, version))
  }

  override def hashCode: Int = this.id.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id == this.id

}

object Channel {

  val HEX_GREEN: String = "#2ECC40"

}
