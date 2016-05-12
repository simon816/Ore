package controllers

import controllers.Requests._
import controllers.project.routes._
import db.ModelService
import db.impl.{ProjectBase, UserBase}
import forums.DiscourseApi
import models.project.Project
import models.user.User
import ore.permission.scope.GlobalScope
import ore.permission.{HideProjects, Permission}
import ore.project.util.{InvalidPluginFileException, ProjectFactory}
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * A set of actions used by Ore.
  */
trait Actions {

  val forums: DiscourseApi
  val users: UserBase
  val projects: ProjectBase

  /**
    * Retrieves, processes, and adds a [[Project]] to a request.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Request with a project if found, NotFound otherwise.
    */
  def ProjectAction(author: String, slug: String) = Action andThen projectAction(author, slug)


  /** Ensures a request is authenticated */
  def Authenticated = Action andThen authAction

  /** Called when a [[User]] tries to make a request they do not have permission for */
  def onUnauthorized(request: RequestHeader) = {
    if (request.flash.get("noRedirect").isEmpty && this.users.current(request.session).isEmpty)
      Redirect(routes.Users.logIn(None, None, Some(request.path)))
    else Redirect(routes.Application.showHome(None, None, None, None))
  }

  /**
    * Ensures a request is authenticated and retrieves, processes, and adds a
    * [[Project]] to a request.
    *
    * @param author Project owner
    * @param slug Project slug
    * @return Authenticated request with a project if found, NotFound otherwise.
    */
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

  private def projectAction(author: String, slug: String)
  = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      projects.withSlug(author, slug)
        .flatMap(processProject(_, users.current(request.session)))
        .map(new ProjectRequest[A](_, request))
        .toRight(NotFound)
    }
  }

  private def processProject(project: Project, user: Option[User]): Option[Project] = {
    if (project.isVisible || (user.isDefined && (user.get can HideProjects in GlobalScope))) {
      if (project.topicId.isEmpty) this.forums.embed.createTopic(project)
      Some(project)
    } else {
      None
    }
  }

  private def authAction = new ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = Future.successful {
      users.current(request.session)
        .map(new AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  private def authedProjectAction(author: String, slug: String)
  = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
    def refine[A](request: AuthRequest[A]) = Future.successful {
      projects.withSlug(author, slug)
        .flatMap(processProject(_, Some(request.user)))
        .map(new AuthedProjectRequest[A](_, request))
        .toRight(NotFound)
    }
  }

}
