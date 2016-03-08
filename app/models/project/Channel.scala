package models.project

import com.google.common.base.Objects
import models.author.Author
import models.project.Channel._

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * TODO: Max channels per-project
  *
  * @param project Project channel belongs to
  * @param name Name of channel
  */
case class Channel(project: Project, name: String, colorHex: String) {

  def this(project: Project, name: String) = this(project, name, HEX_GREEN)

  /**
    * Returns the Version in this channel with the specified version string.
    *
    * @param version Version string
    * @return Version, if any, None otherwise
    */
  def getVersion(version: String): Option[Version] = Version.get(this, version)

  /**
    * Creates a new version within this Channel.
    *
    * @param version Version string
    * @return New channel
    */
  def newVersion(version: String): Version = Version(this, version) // TODO: Add to DB here

  override def hashCode: Int = Objects.hashCode(this.project, this.name)

  override def equals(o: Any): Boolean = {
    o match {
      case that: Channel => that.project.equals(this.project) && that.name.equals(this.name)
      case _ => false
    }
  }

}

/**
  * Channel data-store
  */
object Channel {

  val HEX_GREEN: String = "#2ECC40"

  // TODO: Replace with DB
  val channels = Set[Channel](
    new Channel(Author.get("SpongePowered").getProject("Ore").get, "Alpha"),
    new Channel(Author.get("SpongePowered").getProject("Ore").get, "Beta"),
    new Channel(Author.get("Author1").getProject("Example-1").get, "Alpha"),
    new Channel(Author.get("Author1").getProject("Example-1").get, "Beta"),
    new Channel(Author.get("Author2").getProject("Example-2").get, "Alpha"),
    new Channel(Author.get("Author2").getProject("Example-2").get, "Beta"),
    new Channel(Author.get("Author3").getProject("Example-3").get, "Alpha"),
    new Channel(Author.get("Author3").getProject("Example-3").get, "Beta"),
    new Channel(Author.get("Author4").getProject("Example-4").get, "Alpha"),
    new Channel(Author.get("Author4").getProject("Example-4").get, "Beta"),
    new Channel(Author.get("Author5").getProject("Example-5").get, "Alpha"),
    new Channel(Author.get("Author5").getProject("Example-5").get, "Beta")
  )

  /**
    * Returns the channel belonging to the specified Project with the specified
    * name.
    *
    * @param project Project name
    * @param name Channel name
    * @return Channel if present, None otherwise
    */
  protected[project] def get(project: Project, name: String): Option[Channel] = {
    this.channels.find(channel => channel.project.equals(project) && channel.name.equals(name))
  }

  /**
    * Returns all channels for the specified Project.
    *
    * @param project Project to get channels for
    * @return All channels in project
    */
  protected[project] def getAll(project: Project): Set[Channel] = {
    this.channels.filter(channel => channel.project.equals(project))
  }

}
