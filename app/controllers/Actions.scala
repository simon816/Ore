package controllers

import controllers.Requests.{AuthRequest, AuthedProjectRequest, ProjectRequest, ScopedRequest}
import db.ModelService
import forums.DiscourseApi
import models.project.Project
import models.user.User
import ore.UserBase
import ore.permission.scope.GlobalScope
import ore.permission.{HideProjects, Permission}
import ore.project.ProjectBase
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Represents a controller with user authentication / authorization.
  */
trait Actions {

  val service: ModelService
  val forums: DiscourseApi
  val users: UserBase
  val projects: ProjectBase

  def onUnauthorized(request: RequestHeader) = {
    if (request.flash.get("noRedirect").isEmpty && users.current(request.session).isEmpty)
      Redirect(routes.Users.logIn(None, None, Some(request.path)))
    else Redirect(routes.Application.showHome(None, None, None))
  }

  private def processProject(project: Project, user: Option[User]): Option[Project] = {
    if (project.isVisible || (user.isDefined && (user.get can HideProjects in GlobalScope))) {
      if (project.topicId.isEmpty) forums.embed.createTopic(project)
      Some(project)
    } else {
      None
    }
  }

  private def projectAction(author: String, slug: String)
  = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      projects.withSlug(author, slug)
        .flatMap(processProject(_, users.current(request.session)))
        .map(new ProjectRequest[A](_, request))
        .toRight(NotFound)
    }
  }

  def ProjectAction(author: String, slug: String) = Action andThen projectAction(author, slug)

  // Auth

  def authAction = new ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = Future.successful {
      users.current(request.session)
        .map(new AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  def Authenticated = Action andThen authAction

  // Permissions

  private def authedProjectAction(author: String, slug: String)
  = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
      def refine[A](request: AuthRequest[A]) = Future.successful {
        projects.withSlug(author, slug)
          .flatMap(processProject(_, Some(request.user)))
          .map(new AuthedProjectRequest[A](_, request))
          .toRight(NotFound)
      }
  }

  def AuthedProjectAction(author: String, slug: String) = Authenticated andThen authedProjectAction(author, slug)

  /**
    * Action to perform a permission check for the current ScopedRequest and
    * given Permission.
    *
    * @param p  Permission to check
    * @tparam R Type of ScopedRequest that is being checked
    * @return   The ScopedRequest as an instance of R
    */
  def PermissionAction[R[_] <: ScopedRequest[_]](p: Permission) = new ActionRefiner[ScopedRequest, R] {
    def refine[A](request: ScopedRequest[A]) = Future.successful {
      if (!(request.user can p in request.subject)) {
        Left(onUnauthorized(request))
      } else {
        Right(request.asInstanceOf[R[A]])
      }
    }
  }

  /**
    * A PermissionAction that uses an AuthedProjectRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   An [[AuthedProjectRequest]]
    */
  def ProjectPermissionAction(p: Permission) = PermissionAction[AuthedProjectRequest](p)

}
