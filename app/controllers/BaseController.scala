package controllers

import db.OrePostgresDriver.api._
import db.query.Queries
import models.project.{Channel, Project, Version}
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.DataUtils
import util.forums.SpongeForums

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit ws: WSClient) extends Controller with I18nSupport with Actions {

  SpongeForums.apply
  DataUtils.apply

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
    project.channels.find(Queries.Channels.NameFilter(channelName)) match {
      case None => NotFound
      case Some(channel) => channel.versions.find(_.versionString === versionString) match {
        case None => NotFound
        case Some(version) => f(channel, version)
      }
    }
  }

}
