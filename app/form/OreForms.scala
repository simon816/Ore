package form

import javax.inject.Inject

import form.organization.{OrganizationAvatarUpdate, OrganizationMembersUpdate, OrganizationRoleSetBuilder}
import form.project.{ChannelData, ProjectRoleSetBuilder, ProjectSettings, VersionData}
import models.project.Channel
import models.project.Page._
import models.user.role.ProjectRole
import ore.OreConfig
import ore.project.factory.ProjectFactory
import play.api.data.Form
import play.api.data.Forms._

/**
  * Collection of forms used in this application.
  */
//noinspection ConvertibleToMethodValue
class OreForms @Inject()(implicit config: OreConfig, factory: ProjectFactory) {

  /**
    * Submits a member to be removed from a Project.
    */
  lazy val ProjectMemberRemove = Form(single("username" -> nonEmptyText))

  /**
    * Submits changes to a [[models.project.Project]]'s
    * [[ProjectRole]]s.
    */
  lazy val ProjectMemberRoles = Form(mapping(
    "users" -> list(number),
    "roles" -> list(text)
  )(ProjectRoleSetBuilder.apply)(ProjectRoleSetBuilder.unapply))

  /**
    * Submits a flag on a project for further review.
    */
  lazy val ProjectFlag = Form(single("flag-reason" -> number))

  /**
    * Submits settings changes for a Project.
    */
  lazy val ProjectSave = Form(mapping(
    "category" -> text,
    "issues" -> text,
    "source" -> text,
    "description" -> text,
    "users" -> list(number),
    "roles" -> list(text),
    "userUps" -> list(text),
    "roleUps" -> list(text),
    "update-icon" -> boolean,
    "owner" -> optional(number)
  )(ProjectSettings.apply)(ProjectSettings.unapply))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits a post reply for a project discussion.
    */
  lazy val ProjectReply = Form(single("content" -> text(minLength = MinLength, maxLength = MaxLength)))

  /**
    * Submits a list of organization members to be invited.
    */
  lazy val OrganizationCreate = Form(mapping(
    "name" -> nonEmptyText,
    "users" -> list(number),
    "roles" -> list(text)
  )(OrganizationRoleSetBuilder.apply)(OrganizationRoleSetBuilder.unapply))

  /**
    * Submits an avatar update for an [[models.user.Organization]].
    */
  lazy val OrganizationUpdateAvatar = Form(mapping(
    "avatar-method" -> nonEmptyText,
    "avatar-url" -> optional(nonEmptyText)
  )(OrganizationAvatarUpdate.apply)(OrganizationAvatarUpdate.unapply))

  /**
    * Submits an organization member for removal.
    */
  lazy val OrganizationMemberRemove = Form(single("username" -> nonEmptyText))

  /**
    * Submits a list of members to be added or updated.
    */
  lazy val OrganizationUpdateMembers = Form(mapping(
    "users" -> list(number),
    "roles" -> list(text),
    "userUps" -> list(text),
    "roleUps" -> list(text)
  )(OrganizationMembersUpdate.apply)(OrganizationMembersUpdate.unapply))

  /**
    * Submits a new Channel for a Project.
    */
  lazy val ChannelEdit = Form(mapping(
    "channel-input" -> text.verifying(
      "Invalid channel name.", config.isValidChannelName(_)
    ),

    "channel-color-input" -> text.verifying(
      "Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c))
    )
  )(ChannelData.apply)(ChannelData.unapply))

  /**
    * Submits changes on a documentation page.
    */
  lazy val PageEdit = Form(single(
    "content" -> text(
      minLength = MinLength,
      maxLength = MaxLength
    )))

  /**
    * Submits a tagline change for a User.
    */
  lazy val UserTagline = Form(single("tagline" -> text))

  /**
    * Submits a new Version.
    */
  lazy val VersionCreate = Form(mapping(
    "recommended" -> boolean,

    "channel-input" -> text.verifying(
      "Invalid channel name.", config.isValidChannelName(_)
    ),

    "channel-color-input" -> text.verifying(
      "Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c))
    )
  )(VersionData.apply)(VersionData.unapply))


  /**
    * Submits a change to a Version's description.
    */
  lazy val VersionDescription = Form(single("content" -> text))

}
