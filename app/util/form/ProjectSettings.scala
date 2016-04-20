package util.form

import models.project.Project
import ore.project.Categories
import util.Input._

/**
  * Represents the configurable Project settings that can be submitted via a
  * form.
  */
case class ProjectSettings(val          categoryName: String,
                           val          issues: String,
                           val          source: String,
                           val          description: String,
                           override val users: List[Int],
                           override val roles: List[String])
                           extends      TraitProjectRoleSetBuilder {

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
    val roles = project.roles
    for (role <- this.build()) {
      roles.add(role.copy(projectId=project.id.get))
    }
  }

}
