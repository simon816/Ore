package controllers

import javax.inject.Inject

import db.ModelService
import forums.DiscourseApi
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import util.{OreConfig, OreEnv}
import views.{html => views}

class Organizations @Inject()(implicit override val env: OreEnv,
                              implicit override val config: OreConfig,
                              implicit override val service: ModelService,
                              implicit override val forums: DiscourseApi,
                              implicit override val messagesApi: MessagesApi) extends BaseController {

  def showCreator() = Action { implicit request =>
    Ok(views.organizations.create())
  }

}
