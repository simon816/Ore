package util

import db.Storage
import models.auth.User
import models.project.{Project, Version}
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Helper class for handling tracking of statistics statistic. All operations
  * within this class are handled asynchronously as to not block the request.
  */
object Statistics {

  /**
    * Signifies that a project has been viewed with the specified request and
    * actions should be taken to check whether a view should be added to the
    * Project's view count.
    *
    * @param project Project to check views for
    * @param request Request to view the project
    */
  def projectViewed(project: Project, request: RequestHeader) = {
    cookieOrUser(request,
      cookie => {
        Storage.hasProjectBeenViewedBy(project, cookie).onSuccess {
          case viewed: Boolean => if (!viewed) {
            project.addView()
            Storage.setProjectViewedBy(project, cookie)
          }
        }
      },
      user => {
        Storage.hasProjectBeenViewedBy(project, user).onSuccess {
          case viewed: Boolean => if (!viewed) {
            project.addView()
            Storage.setProjectViewedBy(project, user)
          }
        }
      }
    )
  }

  /**
    * Signifies that a version has been downloaded with the specified request
    * and actions should be taken to check whether a view should be added to
    * the Version's (and Project's) download count.
    *
    * @param version Version to check downloads for
    * @param request Request to download the version
    */
  def versionDownloaded(project: Project, version: Version, request: RequestHeader) = {
    cookieOrUser(request,
      cookie => {
        Storage.hasVersionBeenDownloadedBy(version, cookie).onSuccess {
          case viewed: Boolean => if (!viewed) {
            version.addDownload()
            project.addDownload()
            Storage.setVersionDownloadedBy(version, cookie)
          }
        }
      },
      user => {
        Storage.hasVersionBeenDownloadedBy(version, user).onSuccess {
          case viewed: Boolean => if (!viewed) {
            version.addDownload()
            project.addDownload()
            Storage.setVersionDownloadedBy(version, user)
          }
        }
      }
    )
  }

  private def cookieOrUser(request: RequestHeader, withCookie: String => Unit, withUser: User => Unit) = {
    request.session.get("username") match {
      case None => request.cookies.get("uid") match {
        case None => ;
        case Some(cookie) => withCookie(cookie.value)
      }
      case Some(username) => Storage.getUser(username).onSuccess {
        case user: User => withUser(user)
      }
    }
  }

}
