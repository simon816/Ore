package controllers

import db.ModelService
import forums.DiscourseApi
import models.project.{Project, Version}
import ore.UserBase
import ore.project.ProjectBase
import play.api.i18n.I18nSupport
import play.api.mvc._
import util.OreConfig
import util.StringUtils.equalsIgnoreCase

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit val config: OreConfig,
                              override val service: ModelService,
                              override val users: UserBase,
                              override val projects: ProjectBase,
                              override val forums: DiscourseApi)
                              extends Controller
                                with I18nSupport
                                with Actions {

  /**
    * Executes the given function with the specified result or returns a
    * NotFound if not found.
    *
    * @param author   Project author
    * @param slug     Project slug
    * @param fn       Function to execute
    * @param request  Incoming request
    * @return         NotFound or function result
    */
  def withProject(author: String, slug: String)(fn: Project => Result)(implicit request: RequestHeader): Result
  = projects.withSlug(author, slug).map(fn).getOrElse(NotFound)

  /**
    * Executes the given function with the specified result or returns a
    * NotFound if not found.
    *
    * @param versionString  VersionString
    * @param fn             Function to execute
    * @param request        Incoming request
    * @param project        Project to get version from
    * @return               NotFound or function result
    */
  def withVersion(versionString: String)(fn: Version => Result)
                 (implicit request: RequestHeader, project: Project): Result
  = project.versions.find(equalsIgnoreCase(_.versionString, versionString)).map(fn).getOrElse(NotFound)

}
