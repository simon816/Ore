package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.access.OrganizationBase
import form.OreForms
import forums.DiscourseApi
import ore.permission.EditSettings
import ore.rest.OreWrites
import ore.user.MembershipDossier._
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import views.{html => views}

/**
  * Controller for handling Organization based actions.
  */
class Organizations @Inject()(forms: OreForms,
                              writes: OreWrites,
                              implicit override val env: OreEnv,
                              implicit override val config: OreConfig,
                              implicit override val service: ModelService,
                              implicit override val forums: DiscourseApi,
                              implicit override val messagesApi: MessagesApi) extends BaseController {

  private def EditOrganizationAction(organization: String)
  = AuthedOrganizationAction(organization) andThen OrganizationPermissionAction(EditSettings)

  /**
    * Shows the creation panel for Organizations.
    *
    * @return Organization creation panel
    */
  def showCreator() = Authenticated { implicit request =>
    Ok(views.createOrganization())
  }

  /**
    * Creates a new organization from the submitted data.
    *
    * @return Redirect to organization page
    */
  def create() = Authenticated { implicit request =>
    val formData = this.forms.OrganizationCreate.bindFromRequest().get
    val name = formData.name
    this.service.getModelBase(classOf[OrganizationBase]).create(name, request.user.id.get, formData.build())
    Redirect(routes.Users.showProjects(name, None))
  }

  /**
    * Sets the status of a pending Organization invite for the current user.
    *
    * @param id     Invite ID
    * @param status Invite status
    * @return       NotFound if invite doesn't exist, Ok otherwise
    */
  def setInviteStatus(id: Int, status: String) = Authenticated { implicit request =>
    val user = request.user
    user.organizationRoles.get(id) match {
      case None =>
        NotFound
      case Some(role) =>
        val dossier = role.organization.memberships
        status match {
          case STATUS_DECLINE =>
            dossier.removeRole(role)
            Ok
          case STATUS_ACCEPT =>
            role.setAccepted(true)
            Ok
          case STATUS_UNACCEPT =>
            role.setAccepted(false)
            Ok
          case _ =>
            BadRequest
        }
    }
  }

  /**
    * Updates an [[models.user.Organization]]'s avatar.
    *
    * @param organization Organization to update avatar of
    * @return             Json response with errors if any
    */
  def updateAvatar(organization: String) = EditOrganizationAction(organization) { implicit request =>

    import this.writes._

    def respond(errorsOpt: Option[List[String]]) = errorsOpt match {
      case None =>
        // send the user object
        Ok(Json.toJson(request.organization.toUser.refresh()))
      case Some(errors) =>
        // send errors
        Ok(Json.obj("errors" -> errors))
    }

    val formData = this.forms.OrganizationUpdateAvatar.bindFromRequest.get
    if (formData.isFileUpload) {
      request.body.asMultipartFormData.get.file("avatar-file") match {
        case None =>
          BadRequest
        case Some(file) =>
          respond(this.forums.setAvatar(organization, file.filename, file.ref.file))
      }
    } else {
      respond(this.forums.setAvatar(organization, formData.url.get))
    }
  }

  /**
    * Removes a member from an [[models.user.Organization]].
    *
    * @param organization Organization to update
    * @return             Redirect to Organization page
    */
  def removeMember(organization: String) = EditOrganizationAction(organization) { implicit request =>
    // TODO: Validation!
    this.users.withName(this.forms.OrganizationMemberRemove.bindFromRequest.get.trim) match {
      case None =>
        BadRequest
      case Some(user) =>
        request.organization.memberships.removeMember(user)
        Redirect(routes.Users.showProjects(organization, None))
    }
  }

  /**
    * Updates an [[models.user.Organization]]'s members.
    *
    * @param organization Organization to update
    * @return             Redirect to Organization page
    */
  def updateMembers(organization: String) = EditOrganizationAction(organization) { implicit request =>
    // TODO: Validation!
    this.forms.OrganizationUpdateMembers.bindFromRequest.get.saveTo(request.organization)
    Redirect(routes.Users.showProjects(organization, None))
  }

}
