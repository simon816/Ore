package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.access.OrganizationBase
import discourse.impl.OreDiscourseApi
import form.OreForms
import ore.permission.EditSettings
import ore.rest.OreWrites
import ore.user.MembershipDossier._
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import views.{html => views}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Controller for handling Organization based actions.
  */
class Organizations @Inject()(forms: OreForms,
                              writes: OreWrites,
                              forums: OreDiscourseApi,
                              implicit override val env: OreEnv,
                              implicit override val config: OreConfig,
                              implicit override val service: ModelService,
                              implicit override val messagesApi: MessagesApi) extends BaseController {

  private def EditOrganizationAction(organization: String)
  = AuthedOrganizationAction(organization) andThen OrganizationPermissionAction(EditSettings)

  val createLimit: Int = this.config.orgs.getInt("createLimit").get

  /**
    * Shows the creation panel for Organizations.
    *
    * @return Organization creation panel
    */
  def showCreator() = Authenticated { implicit request =>
    if (request.user.ownedOrganizations.size >= this.createLimit)
      Redirect(routes.Application.showHome(None, None, None, None))
        .flashing("error" -> this.messagesApi("error.org.createLimit", this.createLimit))
    else
      Ok(views.createOrganization())
  }

  /**
    * Creates a new organization from the submitted data.
    *
    * @return Redirect to organization page
    */
  def create() = Authenticated { implicit request =>
    val user = request.user
    if (user.ownedOrganizations.size >= this.createLimit)
      BadRequest
    else {
      val formData = this.forms.OrganizationCreate.bindFromRequest().get
      val name = formData.name
      try {
        this.service.getModelBase(classOf[OrganizationBase]).create(name, user.id.get, formData.build())
        Redirect(routes.Users.showProjects(name, None))
      } catch {
        case e: Exception =>
          // Creation failed
          Redirect(routes.Organizations.showCreator())
            .flashing("error" -> this.messagesApi("error.org.cannotCreate"))
      }
    }
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
    import writes._

    def handleUpdate(future: Future[List[String]]) = {
      val errors = this.forums.await(future.recover {
        case e: Exception =>
          List(this.messagesApi("error.org.cannotUpdateAvatar"))
      })
      if (errors.isEmpty)
        Ok(Json.toJson(request.organization.toUser.refresh()))
      else
        Ok(Json.obj("errors" -> errors))
    }

    val formData = this.forms.OrganizationUpdateAvatar.bindFromRequest.get
    if (formData.isFileUpload) {
      request.body.asMultipartFormData.get.file("avatar-file") match {
        case None =>
          BadRequest
        case Some(file) =>
          handleUpdate(this.forums.setAvatar(organization, file.filename, file.ref.file.toPath))
      }
    } else
      handleUpdate(this.forums.setAvatar(organization, formData.url.get))
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
