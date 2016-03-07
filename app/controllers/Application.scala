package controllers

import javax.inject.Inject

import models.project.Project
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import slick.driver.JdbcProfile
import sql.Storage
import views.{html => views}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(override val messagesApi: MessagesApi, dbConfigProvider: DatabaseConfigProvider)
  extends Controller with I18nSupport {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val storage = new Storage(dbConfig.db)

  println("Projects:")
  dbConfig.db.run(storage.projects.result).map(_.foreach {
    case (id, pluginId, name, description, ownerName, views, downloads, starred) =>
      println("name = " + name)
  })

  dbConfig.db.close()

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action {
    Ok(views.index(Project.projects))
  }

}
