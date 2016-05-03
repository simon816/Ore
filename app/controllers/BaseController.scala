package controllers

import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import db.query.ModelQueries.filterToFunction
import forums.SpongeForums
import models.project.{Channel, Project, Version}
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSClient
import play.api.mvc._
import util.Conf.isDebug
import util.{Conf, DataUtils}

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

  protected[controllers] def withVersion(versionString: String)(fn: Version => Result)
                                        (implicit request: RequestHeader, project: Project): Result
  = project.versions.find(_.versionString.toLowerCase === versionString.toLowerCase).map(fn).getOrElse(NotFound)

}
