package models.project

import com.google.common.base.Objects

/**
  * Represents a single version of a Project.
  *
  * @param project Project this version belongs to
  * @param versionString Version string
  * @param channel Channel this version is in
  */
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

/**
  * Version data-store
  */
object Version {

  // TODO: Replace with DB
  val versions = for (project <- Project.projects) yield Version(project, "1.0.0", project.getChannel("Alpha").get)

  /**
    * Returns the Version belonging to the specified project with the specified
    * version string and channel.
    *
    * @param project Project version belongs to
    * @param versionString Version string
    * @param channel Channel version is in
    * @return Version, if present, None otherwise
    */
  def get(project: Project, versionString: String, channel: Channel): Option[Version] = {
    for (version <- versions) {
      if (version.project.equals(project) && version.versionString.equals(versionString)
        && version.channel.equals(channel)) {
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
  def getAll(project: Project) = for (
    version <- versions
    if version.project.equals(project)
  ) yield {
    version
  }

}
