package models.project

import com.google.common.base.Objects

case class Version(project: Project, versionString: String, channel: Channel) {

  override def hashCode = Objects.hashCode(this.project, this.versionString, this.channel)

  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Version]) {
      return false
    }
    val that = o.asInstanceOf[Version]
    (that.project.equals(this.project) && that.versionString.equals(this.versionString)
      && that.channel.equals(this.channel))
  }

}

object Version {

  // TODO: Replace with DB
  val versions = for (project <- Project.projects) yield Version(project, "1.0.0", project.getChannel("Alpha").get)

  def get(project: Project, versionString: String, channel: Channel): Option[Version] = {
    for (version <- versions) {
      if (version.project.equals(project) && version.versionString.equals(versionString)
        && version.channel.equals(channel)) {
        return Some(version)
      }
    }
    None
  }

}
