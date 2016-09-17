package ore.project

import db.impl.access.ProjectBase
import models.project.Project

/**
  * Represents anything that has a [[models.project.Project]].
  */
trait ProjectOwned {
  /** Returns the Project ID */
  def projectId: Int
  /** Returns the Project */
  def project(implicit projects: ProjectBase): Project = projects.get(this.projectId).get
}
