package models.project

import com.google.common.base.Objects

case class Channel(project: Project, name: String) {

  override def hashCode = Objects.hashCode(this.project, this.name)

  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Channel]) {
      return false
    }
    val that = o.asInstanceOf[Channel]
    that.project.equals(this.project) && that.name.equals(this.name)
  }

}

object Channel {

  // TODO: Replace with DB
  val channels = List(
    Channel(Project.get("example1", "Example-1").get, "Alpha"),
    Channel(Project.get("example1", "Example-1").get, "Beta"),
    Channel(Project.get("example2", "Example-2").get, "Alpha"),
    Channel(Project.get("example2", "Example-2").get, "Beta"),
    Channel(Project.get("example3", "Example-3").get, "Alpha"),
    Channel(Project.get("example3", "Example-3").get, "Beta"),
    Channel(Project.get("example4", "Example-4").get, "Alpha"),
    Channel(Project.get("example4", "Example-4").get, "Beta"),
    Channel(Project.get("example5", "Example-5").get, "Alpha"),
    Channel(Project.get("example5", "Example-5").get, "Beta")
  )

  def get(project: Project, name: String): Option[Channel] = {
    for (channel <- channels) {
      if (channel.project.equals(project) && channel.name.equals(name)) {
        return Some(channel)
      }
    }
    None
  }

}