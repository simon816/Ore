package util

import db.query.Queries
import models.auth.User
import models.project.{Project, Version}
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Helper class for handling tracking of statistics.
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
        Queries.Projects.hasBeenViewedBy(project.id.get, cookie).onSuccess {
          case viewed: Boolean => if (!viewed) {
            project.addView()
            Queries.Projects.setViewedBy(project.id.get, cookie)
          }
        }
      },
      user => {
        Queries.Projects.hasBeenViewedBy(project.id.get, user.externalId).onSuccess {
          case viewed: Boolean => if (!viewed) {
            project.addView()
            Queries.Projects.setViewedBy(project.id.get, user.externalId)
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
        Queries.Versions.hasBeenDownloadedBy(version.id.get, cookie).onSuccess {
          case viewed: Boolean => if (!viewed) {
            version.addDownload()
            project.addDownload()
            Queries.Versions.setDownloadedBy(version.id.get, cookie)
          }
        }
      },
      user => {
        Queries.Versions.hasBeenDownloadedBy(version.id.get, user.externalId).onSuccess {
          case viewed: Boolean => if (!viewed) {
            version.addDownload()
            project.addDownload()
            Queries.Versions.setDownloadedBy(version.id.get, user.externalId)
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
      case Some(username) => withUser(User.withName(username).get)
    }
  }

}
