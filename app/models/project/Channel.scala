package models.project

import com.google.common.base.Objects
import models.project.Channel._

/**
  * Represents a release channel for Project Versions. Each project gets it's
  * own set of channels.
  *
  * @param project Project channel belongs to
  * @param name Name of channel
  */
case class Channel(project: Project, name: String, colorHex: String) {

  def this(project: Project, name: String) = this(project, name, HEX_GREEN)

  override def hashCode = Objects.hashCode(this.project, this.name)

  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Channel]) {
      return false
    }
    val that = o.asInstanceOf[Channel]
    that.project.equals(this.project) && that.name.equals(this.name)
  }

}

/**
  * Channel data-store
  */
object Channel {

  val HEX_GREEN = "#2ECC40"

  // TODO: Replace with DB
  val channels = List(
    new Channel(Project.get("SpongePowered", "Ore").get, "Alpha"),
    new Channel(Project.get("SpongePowered", "Ore").get, "Beta"),
    new Channel(Project.get("Author1", "Example-1").get, "Alpha"),
    new Channel(Project.get("Author1", "Example-1").get, "Beta"),
    new Channel(Project.get("Author2", "Example-2").get, "Alpha"),
    new Channel(Project.get("Author2", "Example-2").get, "Beta"),
    new Channel(Project.get("Author3", "Example-3").get, "Alpha"),
    new Channel(Project.get("Author3", "Example-3").get, "Beta"),
    new Channel(Project.get("Author4", "Example-4").get, "Alpha"),
    new Channel(Project.get("Author4", "Example-4").get, "Beta"),
    new Channel(Project.get("Author5", "Example-5").get, "Alpha"),
    new Channel(Project.get("Author5", "Example-5").get, "Beta")
  )

  /**
    * Returns the channel belonging to the specified Project with the specified
    * name.
    *
    * @param project Project name
    * @param name Channel name
    * @return Channel if present, None otherwise
    */
  def get(project: Project, name: String): Option[Channel] = {
    for (channel <- channels) {
      if (channel.project.equals(project) && channel.name.equals(name)) {
        return Some(channel)
      }
    }
    None
  }

  /**
    * Returns all channels for the specified Project.
    *
    * @param project Project to get channels for
    * @return All channels in project
    */
  def getAll(project: Project) = for (
    channel <- channels
    if channel.project.equals(project)
  ) yield {
    channel
  }

}
