package ore

import models.project.Project
import ore.project.ProjectBase

/** Represents anything that has a [[models.project.Project]]. */
trait ProjectOwner {
  /** Returns the Project ID */
  def projectId: Int
  /** Returns the Project */
  def project(implicit projects: ProjectBase): Project = projects.access.get(this.projectId).get
}
