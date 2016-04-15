package controllers

import models.project.Project
import models.user.User
import ore.permission.Permission
import ore.permission.scope.ScopeSubject
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future

/**
  * Represents a controller with user authentication / authorization.
  */
trait Secured {

  private def onUnauthorized(request: RequestHeader) = {
    Redirect(routes.Application.logIn(None, None, Some(request.path)))
  }

  // Auth

  /**
    * A request that hold the currently authenticated [[User]].
    *
    * @param user     Authenticated user
    * @param request  Request to wrap
    */
  case class AuthRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

  /** Action that ensures that the request is authenticated. */
  object Authenticated extends ActionBuilder[AuthRequest] with ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      User.current(request.session)
        .map(new AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  // Permissions

  /** Represents a Request with a [[User]] and [[ScopeSubject]] */
  trait ScopedRequest[A] extends WrappedRequest[A] {
    def user: User
    def subject: ScopeSubject
  }

  /**
    * A request that hold a Project and a [[AuthRequest]].
    *
    * @param project Project to hold
    * @param request An [[AuthRequest]]
    */
  class AuthedProjectRequest[A](val project: Project, request: AuthRequest[A])
                          extends WrappedRequest[A](request) with ScopedRequest[A] {
    override def user: User = request.user
    override val subject = this.project
  }

  /**
    * Action to retrieve a [[Project]] after authentication has been performed.
    *
    * @param author     Project owner
    * @param slug       Project slug
    * @param countView  True if should count as project view
    * @return           Either a new [[AuthedProjectRequest]] or a [[NotFound]]
    *                   result.
    */
  def AuthedProjectAction(author: String, slug: String, countView: Boolean = false)
    = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
      def refine[A](request: AuthRequest[A]) = Future.successful {
        Project.withSlug(author, slug).map { project =>
          new AuthedProjectRequest(project, request)
        } toRight {
          NotFound
        }
      }
  }

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
