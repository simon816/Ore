package models.project

import java.sql.Timestamp

import com.google.common.base.Preconditions._
import db.impl.ProjectSettingsTable
import db.impl.model.OreModel
import db.impl.model.common.Describable
import db.impl.table.ModelKeys
import db.impl.table.ModelKeys._
import form.project.ProjectSettingsForm
import models.user.Notification
import models.user.role.ProjectRole
import ore.permission.role.RoleTypes
import ore.project.Categories.Category
import ore.project.{Categories, ProjectOwned}
import ore.user.notification.NotificationTypes
import play.api.i18n.MessagesApi
import util.StringUtils._

/**
  * Represents a [[Project]]'s settings.
  *
  * @param id           Unique ID
  * @param createdAt    Instant of creation
  * @param projectId    ID of project settings belong to
  * @param homepage     Project homepage
  * @param _category    Project category
  * @param _issues      Project issues URL
  * @param _source      Project source URL
  * @param _licenseName Project license name
  * @param _licenseUrl  Project license URL
  * @param _description Project description
  */
case class ProjectSettings(override val id: Option[Int] = None,
                           override val createdAt: Option[Timestamp] = None,
                           override val projectId: Int = -1,
                           homepage: Option[String] = None,
                           private var _category: Category = Categories.Undefined,
                           private var _issues: Option[String] = None,
                           private var _source: Option[String] = None,
                           private var _licenseName: Option[String] = None,
                           private var _licenseUrl: Option[String] = None,
                           private var _description: Option[String] = None)
                           extends OreModel(id, createdAt) with ProjectOwned with Describable {

  override type M = ProjectSettings
  override type T = ProjectSettingsTable

  /**
    * Returns the [[Project]]'s [[Category]].
    *
    * @return Project category
    */
  def category: Category = this._category

  /**
    * Sets the [[Project]]'s [[Category]].
    *
    * @param category Project category
    */
  def category_=(category: Category) = {
    checkNotNull(category, "null category", "")
    this._category = category
    if (isDefined) update(ModelKeys.Category)
  }

  /**
    * Returns the [[Project]]'s issues URL.
    *
    * @return Issues URL
    */
  def issues: Option[String] = this._issues

  /**
    * Sets the [[Project]]'s issues URL.
    *
    * @param issues Issues URL
    */
  def issues_=(issues: String) = {
    this._issues = Option(issues)
    if (isDefined) update(Issues)
  }

  /**
    * Returns the [[Project]]'s source code URL.
    *
    * @return Source code URL
    */
  def source: Option[String] = this._source

  /**
    * Sets the [[Project]]'s source code URL.
    *
    * @param source Source code URL
    */
  def source_=(source: String) = {
    this._source = Option(source)
    if (isDefined) update(Source)
  }

  /**
    * Returns the name of the [[Project]]'s license.
    *
    * @return Name of license
    */
  def licenseName: Option[String] = this._licenseName

  /**
    * Sets the name of the [[Project]]'s license.
    *
    * @param licenseName Name of license
    */
  def licenseName_=(licenseName: String) = {
    this._licenseName = Option(licenseName)
    if (isDefined) update(LicenseName)
  }

  /**
    * Returns the URL to the [[Project]]'s license.
    *
    * @return URL to project license
    */
  def licenseUrl: Option[String] = this._licenseUrl

  /**
    * Sets the URL to the [[Project]]'s license.
    *
    * @param licenseUrl URL to project license
    */
  def licenseUrl_=(licenseUrl: String) = {
    this._licenseUrl = Option(licenseUrl)
    if (isDefined) update(LicenseUrl)
  }

  /**
    * Returns the [[Project]]'s description.
    *
    * @return Project description
    */
  override def description: Option[String] = this._description

  /**
    * Sets the [[Project]]'s description.
    *
    * @param description Project description
    */
  def description_=(description: String) = {
    this._description = Option(description)
    if (isDefined) {
      update(Description)
      val project = this.project
      if (project.topicId != -1)
        this.forums.updateProjectTopic(project)
    }
  }

  /**
    * Saves a submitted [[ProjectSettingsForm]] to the [[Project]].
    *
    * @param formData Submitted settings
    * @param messages MessagesApi instance
    */
  //noinspection ComparingUnrelatedTypes
  def save(project: Project, formData: ProjectSettingsForm)(implicit messages: MessagesApi) = {
    this.category = Categories.withName(formData.categoryName)
    this.issues = nullIfEmpty(formData.issues)
    this.source = nullIfEmpty(formData.source)
    this.licenseUrl = nullIfEmpty(formData.licenseUrl)
    this.licenseUrl.foreach(url => this.licenseName = formData.licenseName)
    this.description = nullIfEmpty(formData.description)

    // Update the owner if needed
    formData.ownerId.find(_ != project.ownerId).foreach(ownerId => project.owner = this.userBase.get(ownerId).get)

    // Handle member changes
    if (project.isDefined) {
      // Add new roles
      val dossier = project.memberships
      for (role <- formData.build()) {
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
      for ((user, i) <- formData.userUps.zipWithIndex) {
        project.memberships.members.find(_.username.equalsIgnoreCase(user)).foreach { user =>
          user.headRole.roleType = projectRoleTypes.find(_.title.equals(formData.roleUps(i)))
            .getOrElse(throw new RuntimeException("supplied invalid role type"))
        }
      }
    }
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
