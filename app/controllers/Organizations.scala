package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.access.OrganizationBase
import form.OreForms
import forums.DiscourseApi
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi
import views.{html => views}

/**
  * Controller for handling Organization based actions.
  */
class Organizations @Inject()(forms: OreForms,
                              implicit override val env: OreEnv,
                              implicit override val config: OreConfig,
                              implicit override val service: ModelService,
                              implicit override val forums: DiscourseApi,
                              implicit override val messagesApi: MessagesApi) extends BaseController {

  /**
    * Shows the creation panel for Organizations.
    *
    * @return Organization creation panel
    */
  def showCreator() = Authenticated { implicit request =>
    Ok(views.organizations.create())
  }

  /**
    * Creates a new organization from the submitted data.
    *
    * @return Redirect to organization page
    */
  def create() = Authenticated { implicit request =>
    val formData = this.forms.OrganizationCreate.bindFromRequest().get
    val name = formData.name
    this.service.access(classOf[OrganizationBase]).create(name, request.user.id.get, formData.build())
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
          case "decline" =>
            dossier.removeRole(role)
            Ok
          case "accept" =>
            role.setAccepted(true)
            Ok
          case "unaccept" =>
            role.setAccepted(false)
            Ok
          case _ =>
            BadRequest
        }
    }
  }

}
