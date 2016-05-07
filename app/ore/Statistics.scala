package ore

import java.util.UUID

import controllers.Requests.ProjectRequest
import db.model.Models
import models.project.Version
import models.statistic.{ProjectView, VersionDownload}
import play.api.mvc.{Cookie, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Helper class for handling tracking of statistics.
  */
object Statistics {

  val COOKIE_UID = "uid"

  /**
    * Gets or creates a unique ID for tracking statistics based on the browser.
    *
    * @param request  Request with cookie
    * @return         New or existing cookie
    */
  def getStatCookie(implicit request: RequestHeader)
  = request.cookies.get(COOKIE_UID).map(_.value).getOrElse(UUID.randomUUID.toString)

  /**
    * Signifies that a project has been viewed with the specified request and
    * actions should be taken to check whether a view should be added to the
    * Project's view count.
    *
    * @param request Request to view the project
    */
  def projectViewed(f: ProjectRequest[_] => Result)(implicit request: ProjectRequest[_]): Result = {
    val project = request.project
    val statEntry = ProjectView.bindFromRequest
    Models.Projects.Views.record(statEntry).andThen {
      case recorded => if (recorded.get) project.addView()
    }
    f(request).withCookies(Cookie(COOKIE_UID, statEntry.cookie))
  }

  /**
    * Signifies that a version has been downloaded with the specified request
    * and actions should be taken to check whether a view should be added to
    * the Version's (and Project's) download count.
    *
    * @param version Version to check downloads for
    * @param request Request to download the version
    */
  def versionDownloaded(version: Version)(f: ProjectRequest[_] => Result)(implicit request: ProjectRequest[_]): Result = {
    val statEntry = VersionDownload.bindFromRequest(version)
    Models.Versions.Downloads.record(statEntry).andThen {
      case recorded => if (recorded.get) {
        version.addDownload()
        request.project.addDownload()
      }
    }
    f(request).withCookies(Cookie(COOKIE_UID, statEntry.cookie))
  }

}
