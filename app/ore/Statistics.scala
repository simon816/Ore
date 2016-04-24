package ore

import java.util.UUID

import controllers.Requests.ProjectRequest
import db.query.Queries
import models.project.{Project, Version}
import models.user.User
import play.api.mvc.{Cookie, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Helper class for handling tracking of statistics.
  */
object Statistics {

  private val COOKIE_UID = "uid"

  /**
    * Signifies that a project has been viewed with the specified request and
    * actions should be taken to check whether a view should be added to the
    * Project's view count.
    *
    * @param request Request to view the project
    */
  def projectViewed(f: ProjectRequest[_] => Result)(implicit request: ProjectRequest[_]): Result = {
    val project = request.project
    User.current(request.session) match {
      case None => findUidCookie match {
        case None =>
          // No user or cookie, mark new cookie as viewed and add view
          val newCookie = newCookieView(project)
          project.addView()
          // Complete request with new cookie
          f(request).withCookies(Cookie(COOKIE_UID, newCookie))
        case Some(cookie) =>
          // No user with cookie, has the cookie viewed the project?
          viewedByCookie(project, cookie) { viewed =>
            if (!viewed) {
              // Cookie has not viewed project, mark as viewed and add view
              markCookieView(project, cookie)
              project.addView()
            }
          }
          // Complete request
          f(request)
      }

      case Some(user) => findUidCookie match {
        case None =>
          // User without cookie, has the user viewed the project?
          val newCookie = newCookieView(project)
          viewedByUser(project, user) { viewed =>
            if (!viewed) {
              // User has not viewed project, mark as viewed and add view
              markUserView(project, user)
              project.addView()
            }
          }
          // Complete request with new cookie
          f(request).withCookies(Cookie(COOKIE_UID, newCookie))
        case Some(cookie) =>
          // User with cookie, has the user viewed the project?
          viewedByUser(project, user) { viewed =>
            if (!viewed) {
              // User has not viewed the project, has the cookie viewed it?
              viewedByCookie(project, cookie) { viewed =>
                if (!viewed) {
                  // Neither cookie nor user has viewed the project
                  // Mark cookie and user as viewed and add view
                  markCookieView(project, cookie)
                  markUserView(project, user)
                  project.addView()
                } else {
                  // User has not viewed project but cookie has, mark the user as viewed
                  markUserView(project, user)
                }
              }
            } else {
              // User has viewed project but cookie has not, mark the cookie as viewed
              markCookieView(project, cookie)
            }
          }
          // Complete request
          f(request)
      }
    }
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
    val project = request.project
    User.current(request.session) match {
      case None => findUidCookie match {
        case None =>
          // No user or cookie, mark new cookie as downloaded and add download
          val newCookie = newCookieDownload(version)
          version.addDownload()
          project.addDownload()
          // Complete request with new cookie
          f(request).withCookies(Cookie(COOKIE_UID, newCookie))
        case Some(cookie) =>
          // No user with cookie, has the cookie downloaded the version?
          downloadedByCookie(version, cookie) { downloaded =>
            if (!downloaded) {
              // Cookie has not downloaded version, mark as downloaded and add download
              markCookieDownload(version, cookie)
              version.addDownload()
              project.addDownload()
            }
          }
          // Complete request
          f(request)
      }

      case Some(user) => findUidCookie match {
        case None =>
          // User without cookie, has the user downloaded the version?
          val newCookie = newCookieDownload(version)
          downloadedByUser(version, user) { downloaded =>
            if (!downloaded) {
              // User has not downloaded version.
              // Mark user as downloaded and add download
              markUserDownload(version, user)
              version.addDownload()
              project.addDownload()
            }
          }
          // Complete request with new cookie
          f(request).withCookies(Cookie(COOKIE_UID, newCookie))
        case Some(cookie) =>
          // User with cookie, has the user downloaded the version?
          downloadedByUser(version, user) { downloaded =>
            if (!downloaded) {
              // User has not downloaded version, has the cookie?
              downloadedByCookie(version, cookie) { downloaded =>
                if (!downloaded) {
                  // Neither user nor cookie has downloaded.
                  // Mark both as downloaded and add download
                  markCookieDownload(version, cookie)
                  markUserDownload(version, user)
                  version.addDownload()
                  project.addDownload()
                } else {
                  // User has not downloaded version but cookie has
                  // Mark user as downloaded
                  markUserDownload(version, user)
                }
              }
            } else {
              // User has downloaded version, mark cookie
              markCookieDownload(version, cookie)
            }
          }
          // Complete request
          f(request)
      }
    }
  }

  private def findUidCookie(implicit request: RequestHeader): Option[String] = {
    request.cookies.get(COOKIE_UID).map(_.value)
  }

  private def newCookie: String = UUID.randomUUID().toString

  private def newCookieView(project: Project): String = {
    val cookie = newCookie
    markCookieView(project, cookie)
    cookie
  }

  private def viewedByCookie(project: Project, cookie: String)(f: Boolean => Unit) = {
    Queries.Projects.hasBeenViewedBy(project.id.get, cookie).onSuccess {
      case viewed: Boolean => f(viewed)
    }
  }

  private def viewedByUser(project: Project, user: User)(f: Boolean => Unit) = {
    Queries.Projects.hasBeenViewedBy(project.id.get, user.id.get).onSuccess {
      case viewed: Boolean => f(viewed)
    }
  }

  private def markCookieView(project: Project, cookie: String) = Queries.Projects.setViewedBy(project.id.get, cookie)

  private def markUserView(project: Project, user: User) = Queries.Projects.setViewedBy(project.id.get, user.id.get)

  private def newCookieDownload(version: Version): String = {
    val cookie = newCookie
    markCookieDownload(version, cookie)
    cookie
  }

  private def downloadedByCookie(version: Version, cookie: String)(f: Boolean => Unit) = {
    Queries.Versions.hasBeenDownloadedBy(version.id.get, cookie).onSuccess {
      case downloaded: Boolean => f(downloaded)
    }
  }

  private def downloadedByUser(version: Version, user: User)(f: Boolean => Unit) = {
    Queries.Versions.hasBeenDownloadedBy(version.id.get, user.id.get).onSuccess {
      case downloaded: Boolean => f(downloaded)
    }
  }

  private def markCookieDownload(version: Version, cookie: String) = {
    Queries.Versions.setDownloadedBy(version.id.get, cookie)
  }

  private def markUserDownload(version: Version, user: User) = {
    Queries.Versions.setDownloadedBy(version.id.get, user.id.get)
  }

}
