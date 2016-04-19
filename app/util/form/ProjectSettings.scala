package util.form

import models.project.Project
import ore.project.Categories
import util.Input._

/**
  * Represents the configurable Project settings that can be submitted via a
  * form.
  */
case class ProjectSettings(categoryName: String, issues: String, source: String, description: String) {

  /**
    * Saves these settings to the specified [[Project]].
    *
    * @param project Project to save to
    */
  def saveTo(project: Project) = {
    project.category = Categories.withName(categoryName)
    project.issues = nullIfEmpty(issues)
    project.source = nullIfEmpty(source)
    project.description = nullIfEmpty(description)
  }

}
