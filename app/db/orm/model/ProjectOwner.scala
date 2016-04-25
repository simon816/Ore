package db.orm.model

import models.project.Project

trait ProjectOwner {
  def projectId: Int
  def project: Project = Project.withId(this.projectId).get
}
