package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.access.OrganizationBase
import form.OreForms
import forums.DiscourseApi
import play.api.i18n.MessagesApi
import util.{OreConfig, OreEnv}
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

}
