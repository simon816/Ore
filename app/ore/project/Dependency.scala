package ore.project

import db.impl.service.ProjectBase
import models.project.Project

/**
  * Represents a dependency to another plugin. Either on or not on Ore.
  *
  * @param pluginId   Unique plugin ID
  * @param version    Version of dependency
  */
case class Dependency(pluginId: String, version: String) {

  /**
    * Tries to resolve this dependency as a Project and returns the result.
    *
    * @return Project if dependency is on Ore, empty otherwise.
    */
  def project(implicit projects: ProjectBase): Option[Project] = projects.withPluginId(this.pluginId)

}
