package controllers

import javax.inject.Inject

import models.project.Project
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import slick.driver.JdbcProfile
import sql.Storage
import views.{html => views}

class Application @Inject()(override val messagesApi: MessagesApi, dbConfigProvider: DatabaseConfigProvider)
  extends Controller with I18nSupport {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action {
    Ok(views.index(Project.projects))
  }

}
