package form.project

import db.ModelService
import db.impl.access.{ProjectBase, UserBase}
import discourse.impl.OreDiscourseApi
import models.project.Project
import models.user.Notification
import models.user.role.ProjectRole
import ore.OreConfig
import ore.permission.role.RoleTypes
import ore.project.Categories
import ore.user.notification.NotificationTypes
import play.api.i18n.MessagesApi
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
                           updateIcon: Boolean,
                           ownerId: Option[Int])
                           extends TProjectRoleSetBuilder {

  /**
    * Saves these settings to the specified [[Project]].
    *
    * @param project Project to save to
    */
  //noinspection ComparingUnrelatedTypes
  def saveTo(project: Project)(implicit service: ModelService, projects: ProjectBase, forums: OreDiscourseApi,
                               config: OreConfig, messages: MessagesApi, users: UserBase) = {
    project.category = Categories.withName(this.categoryName)
    project.issues = nullIfEmpty(this.issues)
    project.source = nullIfEmpty(this.source)
    project.description = nullIfEmpty(this.description)


    this.ownerId.find(_ != project.ownerId).foreach(ownerId => project.owner = users.get(ownerId).get)

    if (this.updateIcon)
      projects.savePendingIcon(project)

    if (project.isDefined) {
      // Add new roles
      val dossier = project.memberships
      for (role <- this.build()) {
        val user = role.user
        dossier.addRole(role.copy(projectId = project.id.get))
        user.sendNotification(Notification(
          originId = project.ownerId,
          notificationType = NotificationTypes.ProjectInvite,
          message = messages("notification.project.invite", role.roleType.title, project.name)
        ))
      }

      // Update existing roles
      val projectRoleTypes = RoleTypes.values.filter(_.roleClass.equals(classOf[ProjectRole]))
      for ((user, i) <- this.userUps.zipWithIndex) {
        project.memberships.members.find(_.username.equalsIgnoreCase(user)).foreach { user =>
          user.headRole.roleType = projectRoleTypes.find(_.title.equals(roleUps(i)))
            .getOrElse(throw new RuntimeException("supplied invalid role type"))
        }
      }
    }
  }
}
