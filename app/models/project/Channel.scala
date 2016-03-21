package models.project

import java.nio.file.Files
import java.sql.Timestamp

import db.Storage
import models.project.Channel._
import org.spongepowered.plugin.meta.version.ComparableVersion
import org.spongepowered.plugin.meta.version.ComparableVersion.{ListItem, StringItem}
import plugin.ProjectManager

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * TODO: Max channels per-project
  *
  * @param id         Unique identifier
  * @param createdAt  Instant of creation
  * @param name       Name of channel
  * @param colorHex   Hex color
  * @param projectId  ID of project this channel belongs to
  */
case class Channel(id: Option[Int], var createdAt: Option[Timestamp], name: String,
                   colorHex: String, projectId: Int) {

  def this(name: String, projectId: Int) = this(None, None, name, DEFAULT_COLOR, projectId)

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
    * @param version  Version string
    * @return         Version, if any, None otherwise
    */
  def getVersion(version: String): Future[Option[Version]] = Storage.optVersion(this.id.get, version)

  /**
    * Creates a new version within this Channel.
    *
    * @param version  Version string
    * @return         New channel
    */
  def newVersion(version: String, dependencies: List[String], description: String, assets: String): Future[Version] = {
    Storage.createVersion(new Version(version, dependencies, description, assets, this.projectId, this.id.get))
  }

  /**
    * Deletes the specified Version within this channel.
    *
    * @param version  Version to delete
    * @param context  Project for context
    * @return         Result
    */
  def deleteVersion(version: Version, context: Project): Try[Unit] = Try {
    if (context.getVersions.size == 1) {
      throw new IllegalArgumentException("Cannot delete project's lone version.")
    }
    Storage.now(Storage.deleteVersion(version)) match {
      case Failure(thrown) => throw thrown
      case Success(i) =>
        Files.delete(ProjectManager.getUploadPath(context.owner, context.getName, version.versionString, this.name))
    }
  }

  override def hashCode: Int = this.id.get.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Channel] && o.asInstanceOf[Channel].id.get == this.id.get
  }

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
    * @param version  Version string to parse
    * @return         Suggested channel name
    */
  def getSuggestedNameForVersion(version: String): String = {
    firstString(new ComparableVersion(version).getItems).getOrElse(DEFAULT_CHANNEL)
  }

  private def firstString(items: ListItem): Option[String] = {
    // Get the first non-number component in the version string
    var str: Option[String] = None
    var i = 0
    while (str.isEmpty && i < items.size()) {
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
