package controllers

import javax.inject.Inject

import db.ModelService
import db.impl.access.OrganizationBase
import db.impl.service.UserBase
import form.OreForms
import forums.DiscourseApi
import play.api.i18n.MessagesApi
import play.api.mvc.Action
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

  def create() = Authenticated { implicit request =>
    val name = this.forms.OrganizationCreate.bindFromRequest().get
    val org = this.service.access[OrganizationBase](classOf[OrganizationBase]).create(name, request.user.id.get)
    Ok(views.organizations.view(org))
  }

}
