package ore

import db.impl.ProjectBase
import models.project.Project

/** Represents anything that has a [[models.project.Project]]. */
trait ProjectOwned {
  /** Returns the Project ID */
  def projectId: Int
  /** Returns the Project */
  def project(implicit projects: ProjectBase): Project = projects.access.get(this.projectId).get
}
