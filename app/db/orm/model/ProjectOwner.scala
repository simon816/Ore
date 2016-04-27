package db.orm.model

import models.project.Project

/** Represents anything that has a [[models.project.Project]]. */
trait ProjectOwner {
  /** Returns the Project ID */
  def projectId: Int
  /** Returns the Project */
  def project: Project = Project.withId(this.projectId).get
}
