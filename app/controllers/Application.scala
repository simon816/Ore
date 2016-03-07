package controllers

import javax.inject.Inject

import models.project.Project
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import views.{html => views}

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action {
    Ok(views.index(Project.projects))
  }

}
