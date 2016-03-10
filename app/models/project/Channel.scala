package models.project

import java.sql.Timestamp

import models.project.Channel._
import db.Storage

import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure, Try}

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
case class Channel(id: Option[Int], var createdAt: Option[Timestamp], projectId: Int, name: String, colorHex: String) {

  def this(projectId: Int, name: String) = this(None, None, projectId, name, HEX_GREEN)

  /**
    * Returns the Project this Channel belongs to.
    *
    * @return Project the Channel belongs to
    */
  def getProject: Future[Project] = Storage.getProject(this.projectId)

  /**
    * Returns all Versions in this channel.
    *
    * @return All versions
    */
  def getVersions: Future[Seq[Version]] = Storage.getVersions(this.id.get)

  /**
    * Returns the Version in this channel with the specified version string.
    *
    * @param version Version string
    * @return Version, if any, None otherwise
    */
  def getVersion(version: String): Future[Option[Version]] = Storage.optVersion(this.id.get, version)

  /**
    * Creates a new version within this Channel.
    *
    * @param version Version string
    * @return New channel
    */
  def newVersion(version: String): Future[Version] = {
    Storage.createVersion(new Version(this.projectId, this.id.get, version))
  }

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get

}

object Channel {

  val HEX_GREEN: String = "#2ECC40"

}
