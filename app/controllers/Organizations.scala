package controllers

import javax.inject.Inject

import controllers.routes.{Organizations => self}
import db.ModelService
import db.impl.access.OrganizationBase
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

  /**
    * Creates a new organization from the submitted data.
    *
    * @return Redirect to organization page
    */
  def create() = Authenticated { implicit request =>
    val name = this.forms.OrganizationCreate.bindFromRequest().get
    this.service.access[OrganizationBase](classOf[OrganizationBase]).create(name, request.user.id.get)
    Redirect(self.show(name))
  }

  /**
    * Displays an organization page.
    *
    * @param name Organization name
    * @return     Organization page
    */
  def show(name: String) = Action { implicit request =>
    this.service.access(classOf[OrganizationBase]).withName(name) match {
      case None => NotFound
      case Some(org) => Ok(views.organizations.view(org))
    }
  }

}
