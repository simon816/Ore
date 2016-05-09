package controllers

import db.ModelService
import forums.DiscourseApi
import models.project.{Project, Version}
import play.api.i18n.I18nSupport
import play.api.mvc._
import util.StringUtils.equalsIgnoreCase

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit override val service: ModelService,
                              override val forums: DiscourseApi) extends Controller
                                                                   with I18nSupport
                                                                   with Actions {

  def withProject(author: String, slug: String)(f: Project => Result)(implicit request: RequestHeader): Result
  = Project.withSlug(author, slug).map(f).getOrElse(NotFound)

  def withVersion(versionString: String)(fn: Version => Result)
                 (implicit request: RequestHeader, project: Project): Result
  = project.versions.find(equalsIgnoreCase(_.versionString, versionString)).map(fn).getOrElse(NotFound)

}
