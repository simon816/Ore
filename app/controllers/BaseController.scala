package controllers

import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import db.query.ModelQueries.filterToFunction
import forums.SpongeForums
import models.project.{Channel, Project, Version}
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.C.isDebug
import util.{C, DataUtils}

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit ws: WSClient) extends Controller with I18nSupport with Actions {

  SpongeForums.enable()
  if (isDebug) DataUtils.enable()

  protected[controllers] def withProject(author: String, slug: String)(f: Project => Result)
                                        (implicit request: RequestHeader): Result = {
    Project.withSlug(author, slug) match {
      case None => NotFound
      case Some(project) => f(project)
    }
  }

  protected[controllers] def withVersion(channelName: String, versionString: String)
                                        (f: (Channel, Version) => Result)
                                        (implicit request: RequestHeader, project: Project): Result = {
    project.channels.find(ModelQueries.Channels.NameFilter(channelName)) match {
      case None => NotFound
      case Some(channel) => channel.versions.find(_.versionString === versionString) match {
        case None => NotFound
        case Some(version) => f(channel, version)
      }
    }
  }

}
