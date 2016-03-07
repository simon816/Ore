package controllers

import javax.inject.Inject

import com.google.common.base.MoreObjects
import sql.Storage
import models.project.Project
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import slick.driver.JdbcProfile
import views.{html => views}
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(override val messagesApi: MessagesApi, dbConfigProvider: DatabaseConfigProvider)
  extends Controller with I18nSupport {

  val dbConfig = dbConfigProvider.get[JdbcProfile]
  val storage = new Storage(dbConfig.db)

  println("Projects:")
  dbConfig.db.run(storage.projects.result).map(_.foreach {
    case (id, pluginId, name, description, owner, views, downloads, starred) =>
      println(MoreObjects.toStringHelper(classOf[Project])
        .add("id", id)
        .add("pluginId", pluginId)
        .add("name", name)
        .add("description", description)
        .add("owner", owner)
        .add("views", views)
        .add("downloads", downloads)
        .toString
      )
  })

  /**
    * Display the home page.
    *
    * @return Home page
    */
  def index = Action {
    Ok(views.index(Project.projects))
  }

}
