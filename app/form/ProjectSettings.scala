package form

import db.ModelService
import db.impl.service.UserBase
import forums.DiscourseApi
import models.project.Project
import models.user.Notification
import ore.NotificationTypes
import ore.permission.role.RoleTypes
import ore.project.Categories
import ore.project.util.ProjectManager
import play.api.i18n.MessagesApi
import util.OreConfig
import util.StringUtils._

/**
  * Represents the configurable Project settings that can be submitted via a
  * form.
  */
case class ProjectSettings(categoryName: String,
                           issues: String,
                           source: String,
                           description: String,
                           override val users: List[Int],
                           override val roles: List[String],
                           userUps: List[String],
                           roleUps: List[String],
                           updateIcon: Boolean)
                           extends TProjectRoleSetBuilder {

  /**
    * Saves these settings to the specified [[Project]].
    *
    * @param project Project to save to
    */
  def saveTo(project: Project)(implicit service: ModelService, manager: ProjectManager, forums: DiscourseApi,
                               config: OreConfig, messages: MessagesApi, users: UserBase) = {
    project.category = Categories.withName(this.categoryName)
    project.issues = nullIfEmpty(this.issues)
    project.source = nullIfEmpty(this.source)
    project.description = nullIfEmpty(this.description)

    if (this.updateIcon)
      manager.savePendingIcon(project)

    if (project.isDefined) {
      // Add new roles
      val roles = project.roles
      for (role <- this.build()) {
        roles.add(role.copy(projectId = project.id.get))
        role.user.sendNotification(Notification(
          originId = project.ownerId,
          notificationType = NotificationTypes.ProjectInvite,
          message = messages("notification.projectInvite", role.roleType.title, project.name)
        ))
      }

      // Update existing roles
      for ((user, i) <- this.userUps.zipWithIndex) {
        project.members.find(_.name.equalsIgnoreCase(user)).get.headRole.roleType = RoleTypes.withName(roleUps(i))
      }
    }
  }
}
