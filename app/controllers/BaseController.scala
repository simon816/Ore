package controllers

import db.ModelService
import db.impl.VersionTable
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import discourse.DiscourseApi
import discourse.impl.OreDiscourseApi
import models.project.{Project, Version}
import ore.{OreConfig, OreEnv}
import play.api.i18n.I18nSupport
import play.api.mvc._
import util.StringUtils.equalsIgnoreCase

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController(implicit val env: OreEnv,
                              val config: OreConfig,
                              val service: ModelService,
                              override val forums: OreDiscourseApi)
                              extends Controller
                                with Actions
                                with I18nSupport {

  implicit override val users: UserBase = this.service.getModelBase(classOf[UserBase])
  implicit override val projects: ProjectBase = this.service.getModelBase(classOf[ProjectBase])
  implicit val organizations: OrganizationBase = this.service.getModelBase(classOf[OrganizationBase])

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
  = this.projects.withSlug(author, slug).map(fn).getOrElse(NotFound)

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
  = project.versions.find(equalsIgnoreCase[VersionTable](_.versionString, versionString)).map(fn).getOrElse(NotFound)

}
