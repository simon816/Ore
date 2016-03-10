package controllers

import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import db.Storage
import views.{html => views}

import scala.util.{Failure, Success}

class Application @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action {
    Storage.now(Storage.getProjects) match {
      case Failure(thrown) => throw thrown
      case Success(projects) => Ok(views.index(projects))
    }
  }

}
