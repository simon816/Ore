package models.project

import com.google.common.base.Objects

/**
  * Represents a single version of a Project.
  *
  * @param versionString Version string
  * @param channel Channel this version is in
  */
case class Version(channel: Channel, versionString: String) {

  override def hashCode: Int = Objects.hashCode(this.versionString, this.channel)

  override def equals(o: Any): Boolean = {
    if (!o.isInstanceOf[Version]) {
      return false
    }
    val that = o.asInstanceOf[Version]
    that.versionString.equals(this.versionString) && that.channel.equals(this.channel)
  }

}

/**
  * Version data-store
  */
object Version {

  // TODO: Replace with DB
  val versions: Set[Version] = for (project <- Project.projects) yield Version(project.getChannel("Alpha").get, "1.0.0")

  /**
    * Returns the Version belonging to the specified project with the specified
    * version string and channel.
    *
    * @param versionString Version string
    * @param channel Channel version is in
    * @return Version, if present, None otherwise
    */
  def get(channel: Channel, versionString: String): Option[Version] = {
    for (version <- versions) {
      if (version.versionString.equals(versionString) && version.channel.equals(channel)) {
        return Some(version)
      }
    }
    None
  }

  /**
    * Returns all versions for the specified Project.
    *
    * @param project Project to get versions for
    * @return All versions of project
    */
  def getAll(project: Project): Set[Version] = for (
    version <- versions
    if version.channel.project.equals(project)
  ) yield {
    version
  }

}
