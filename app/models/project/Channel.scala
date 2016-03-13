package models.project

import java.sql.Timestamp

import db.Storage
import models.project.Channel._
import org.spongepowered.plugin.meta.version.ComparableVersion
import org.spongepowered.plugin.meta.version.ComparableVersion.{ListItem, StringItem}

import scala.concurrent.Future

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

  def this(projectId: Int, name: String) = this(None, None, projectId, name, DEFAULT_COLOR)

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
  def newVersion(version: String, description: String): Future[Version] = {
    Storage.createVersion(new Version(this.projectId, this.id.get, version, description))
  }

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get

}

object Channel {

  /**
    * The default color used for Channels.
    */
  val DEFAULT_COLOR: String = "#2ECC40"

  /**
    * The default name used for Channels.
    */
  val DEFAULT_CHANNEL: String = "Release"

  /**
    * Attempts to determine a Channel name from the specified version string.
    * This is attained using a ComparableVersion and finding the first
    * StringItem within the parsed version. (e.g. 1.0.0-alpha) would return
    * "alpha".
    *
    * @param version Version string to parse
    * @return Suggested channel name
    */
  def getSuggestedNameForVersion(version: String): String = {
    firstString(new ComparableVersion(version).getItems).getOrElse(DEFAULT_CHANNEL)
  }

  private def firstString(items: ListItem): Option[String] = {
    var str: Option[String] = None
    var i = 0
    while (str.isEmpty) {
      items.get(i) match {
        case item: StringItem => str = Some(item.getValue)
        case item: ListItem => str = firstString(item)
        case _ => ;
      }
      i += 1
    }
    str
  }

}
