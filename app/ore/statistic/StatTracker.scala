package ore.statistic

import java.util.UUID

import controllers.Requests.ProjectRequest
import models.project.Version
import models.statistic.{ProjectView, VersionDownload}
import ore.statistic.StatTracker.COOKIE_UID
import play.api.mvc.{Cookie, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Helper class for handling tracking of statistics.
  */
trait StatTracker {

  /**
    * Signifies that a project has been viewed with the specified request and
    * actions should be taken to check whether a view should be added to the
    * Project's view count.
    *
    * @param request Request to view the project
    */
  def projectViewed(f: ProjectRequest[_] => Result)(implicit request: ProjectRequest[_]): Result = {
    implicit val service = request.service
    val project = request.project
    val statEntry = ProjectView.bindFromRequest
    project.actions.Views.record(statEntry).andThen {
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
    implicit val service = request.service
    val statEntry = VersionDownload.bindFromRequest(version)
    version.actions.Downloads.record(statEntry).andThen {
      case recorded => if (recorded.get) {
        version.addDownload()
        request.project.addDownload()
      }
    }
    f(request).withCookies(Cookie(COOKIE_UID, statEntry.cookie))
  }

}

object StatTracker {

  val COOKIE_UID = "uid"

  /**
    * Gets or creates a unique ID for tracking statistics based on the browser.
    *
    * @param request  Request with cookie
    * @return         New or existing cookie
    */
  def getStatCookie(implicit request: RequestHeader)
  = request.cookies.get(COOKIE_UID).map(_.value).getOrElse(UUID.randomUUID.toString)

}

class OreStatTracker extends StatTracker
