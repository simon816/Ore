package form

import java.net.{MalformedURLException, URL}
import javax.inject.Inject

import controllers.sugar.Requests.ProjectRequest
import db.ModelService
import db.impl.OrePostgresDriver.api._
import form.organization.{OrganizationAvatarUpdate, OrganizationMembersUpdate, OrganizationRoleSetBuilder}
import form.project._
import models.api.ProjectApiKey
import models.project.Channel
import models.project.Page._
import models.user.role.ProjectRole
import ore.OreConfig
import ore.project.factory.ProjectFactory
import ore.rest.ProjectApiKeyTypes
import ore.rest.ProjectApiKeyTypes.ProjectApiKeyType
import play.api.data.{Form, FormError}
import play.api.data.Forms._
import play.api.data.format.Formatter

import scala.util.Try

/**
  * Collection of forms used in this application.
  */
//noinspection ConvertibleToMethodValue
class OreForms @Inject()(implicit config: OreConfig, factory: ProjectFactory, service: ModelService) {

  val url = text verifying("error.url.invalid", text => {
    if (text.isEmpty)
      true
    else {
      try {
        new URL(text)
        true
      } catch {
        case _: MalformedURLException =>
          false
      }
    }
  })

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
  lazy val ProjectFlag = Form(mapping(
    "flag-reason" -> number,
    "comment" -> optional(nonEmptyText))
  (FlagForm.apply)(FlagForm.unapply))

  /**
    * Submits settings changes for a Project.
    */
  lazy val ProjectSave = Form(mapping(
    "category" -> text,
    "issues" -> url,
    "source" -> url,
    "license-name" -> text,
    "license-url" -> url,
    "description" -> text,
    "users" -> list(number),
    "roles" -> list(text),
    "userUps" -> list(text),
    "roleUps" -> list(text),
    "update-icon" -> boolean,
    "owner" -> optional(number)
  )(ProjectSettingsForm.apply)(ProjectSettingsForm.unapply))

  /**
    * Submits a name change for a project.
    */
  lazy val ProjectRename = Form(single("name" -> text))

  /**
    * Submits a post reply for a project discussion.
    */
  lazy val ProjectReply = Form(mapping(
    "content" -> text(minLength = MinLength, maxLength = MaxLength),
    "poster" -> optional(nonEmptyText)
  )(DiscussionReplyForm.apply)(DiscussionReplyForm.unapply))

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
    "avatar-url" -> optional(url)
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
  lazy val PageEdit = Form(mapping(
    "parent-id" -> optional(number),
    "content" -> optional(text(
      maxLength = MaxLength
    )))(PageSaveForm.apply)(PageSaveForm.unapply))

  /**
    * Submits a tagline change for a User.
    */
  lazy val UserTagline = Form(single("tagline" -> text))

  /**
    * Submits a PGP public key update.
    */
  lazy val UserPgpPubKey = Form(mapping(
    "pgp-pub-key" -> text
  )(PGPPublicKeySubmission.apply)(PGPPublicKeySubmission.unapply) verifying("error.invalidKey", _.validateKey()))

  /**
    * Submits a new Version.
    */
  lazy val VersionCreate = Form(mapping(
    "recommended" -> boolean,
    "channel-input" -> text.verifying(
      "Invalid channel name.", config.isValidChannelName(_)),
    "channel-color-input" -> text.verifying(
      "Invalid channel color.", c => Channel.Colors.exists(_.hex.equalsIgnoreCase(c))),
    "content" -> optional(text)
  )(VersionData.apply)(VersionData.unapply))


  /**
    * Submits a change to a Version's description.
    */
  lazy val VersionDescription = Form(single("content" -> text))

  val projectApiKeyType = of[ProjectApiKeyType](new Formatter[ProjectApiKeyType] {
    def bind(key: String, data: Map[String, String]) =
      data.get(key)
        .flatMap(id => Try(id.toInt).toOption.map(ProjectApiKeyTypes(_).asInstanceOf[ProjectApiKeyType]))
        .toRight(Seq(FormError(key, "error.required", Nil)))
    def unbind(key: String, value: ProjectApiKeyType): Map[String, String] = Map(key -> value.id.toString)
  })

  lazy val ProjectApiKeyCreate = Form(single("key-type" -> projectApiKeyType))

  def required(key: String) = Seq(FormError(key, "error.required", Nil))

  val projectApiKey = of[ProjectApiKey](new Formatter[ProjectApiKey] {
    val projectApiKeys = OreForms.this.service.access[ProjectApiKey](classOf[ProjectApiKey])
    def bind(key: String, data: Map[String, String]) =
      data.get(key).
        flatMap(id => Try(id.toInt).toOption.flatMap(this.projectApiKeys.get(_)))
        .toRight(required(key))
    def unbind(key: String, value: ProjectApiKey): Map[String, String] = Map(key -> value.id.get.toString)
  })

  lazy val ProjectApiKeyRevoke = Form(single("id" -> projectApiKey))

  def channel(implicit request: ProjectRequest[_]) = of[Channel](new Formatter[Channel] {
    def bind(key: String, data: Map[String, String]) =
      data.get(key)
        .flatMap(c => request.project.channels.find(_.name.toLowerCase === c.toLowerCase))
        .toRight(Seq(FormError(key, "api.deploy.channelNotFound", Nil)))
    def unbind(key: String, value: Channel) = Map(key -> value.name.toLowerCase)
  })

  def VersionDeploy(implicit request: ProjectRequest[_]) = Form(mapping(
    "apiKey" -> nonEmptyText,
    "channel" -> channel,
    "recommended" -> default(boolean, true))
  (VersionDeployForm.apply)(VersionDeployForm.unapply))

}
