package controllers

import models.project.Project
import pkg.Statistics
import play.api.i18n.I18nSupport
import play.api.mvc.{Controller, RequestHeader, Result}

/**
  * Represents a Secured base Controller for this application.
  */
abstract class BaseController extends Controller with I18nSupport with Secured {

  protected[controllers] def withProject(author: String, slug: String, f: Project => Result,
                                         countView: Boolean = false)(implicit request: RequestHeader): Result = {
    Project.withSlug(author, slug) match {
      case None => NotFound
      case Some(project) =>
        if (countView) {
          Statistics.projectViewed(project, request)
        }
        f(project)
    }
  }

}
