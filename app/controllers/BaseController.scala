package controllers

import models.project.Project
import play.api.i18n.I18nSupport
import play.api.mvc.{Result, Controller}

abstract class BaseController extends Controller with I18nSupport with Secured {

  protected[controllers] def withProject(author: String, slug: String, f: Project => Result): Result = {
    Project.withSlug(author, slug) match {
      case None => NotFound
      case Some(project) => f(project)
    }
  }

}
