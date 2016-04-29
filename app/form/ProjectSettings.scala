package form

import models.project.Project
import ore.permission.role.RoleTypes
import ore.project.Categories
import util.StringUtils._

/**
  * Represents the configurable Project settings that can be submitted via a
  * form.
  */
case class ProjectSettings(val          categoryName: String,
                           val          issues: String,
                           val          source: String,
                           val          description: String,
                           override val users: List[Int],
                           override val roles: List[String],
                           val          userUps: List[String],
                           val          roleUps: List[String])
                           extends      TProjectRoleSetBuilder {

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
    if (project.isDefined) {
      // Add new roles
      val roles = project.roles
      for (role <- this.build()) {
        roles.add(role.copy(projectId = project.id.get))
      }

      // Update existing roles
      for ((user, i) <- this.userUps.zipWithIndex) {
        project.members.find(_.name.equalsIgnoreCase(user)).get.headRole.roleType = RoleTypes.withName(roleUps(i))
      }
    }
  }

}
