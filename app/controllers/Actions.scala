package controllers

import controllers.Requests.{OrganizationRequest, _}
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import models.project.Project
import models.user.User
import ore.permission.scope.GlobalScope
import ore.permission.{EditSettings, HideProjects, Permission}
import play.api.mvc.Results._
import play.api.mvc._
import security.sso.SingleSignOn

import scala.concurrent.Future
import scala.language.higherKinds

/**
  * A set of actions used by Ore.
  */
trait Actions {

  val users: UserBase
  val projects: ProjectBase
  val organizations: OrganizationBase
  val sso: SingleSignOn

  /** Ensures a request is authenticated */
  def Authenticated = Action andThen authAction

  /** Called when a [[User]] tries to make a request they do not have permission for */
  def onUnauthorized(request: RequestHeader) = {
    if (request.flash.get("noRedirect").isEmpty && this.users.current(request.session).isEmpty)
      Redirect(routes.Users.logIn(None, None, Some(request.path)))
    else
      Redirect(routes.Application.showHome(None, None, None, None))
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
      if (!(request.user can p in request.subject))
        Left(onUnauthorized(request))
      else
        Right(request.asInstanceOf[R[A]])
    }
  }

  /**
    * Retrieves, processes, and adds a [[Project]] to a request.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Request with a project if found, NotFound otherwise.
    */
  def ProjectAction(author: String, slug: String) = Action andThen projectAction(author, slug)

  /**
    * Retrieves, processes, and adds a [[Project]] to a request.
    *
    * @param pluginId The project's unique plugin ID
    * @return         Request with a project if found, NotFound otherwise
    */
  def ProjectAction(pluginId: String) = Action andThen projectAction(pluginId)

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
    * A PermissionAction that uses an AuthedProjectRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   An [[AuthedProjectRequest]]
    */
  def ProjectPermissionAction(p: Permission) = PermissionAction[AuthedProjectRequest](p)

  /**
    * Retrieves an [[models.user.Organization]] and adds it to the request.
    *
    * @param organization Organization to retrieve
    * @return             Request with organization if found, NotFound otherwise
    */
  def OrganizationAction(organization: String) = Action andThen organizationAction(organization)

  /**
    * Ensures a request is authenticated and retrieves and adds a
    * [[models.user.Organization]] to the request.
    *
    * @param organization Organization to retrieve
    * @return             Authenticated request with Organization if found, NotFound otherwise
    */
  def AuthedOrganizationAction(organization: String) = Authenticated andThen authedOrganizationAction(organization)

  /**
    * A PermissionAction that uses an AuthedOrganizationRequest for the
    * ScopedRequest.
    *
    * @param p  Permission to check
    * @return   [[AuthedOrganizationRequest]]
    */
  def OrganizationPermissionAction(p: Permission) = PermissionAction[AuthedOrganizationRequest](p)

  /**
    * A request that ensures that a user has permission to edit a specified
    * profile.
    *
    * @param username User to check
    * @return [[AuthRequest]] if has permission
    */
  def UserAction(username: String) = Authenticated andThen userAction(username)

  /**
    * Represents an action that requires a user to reenter their password.
    *
    * @param username Username to verify
    * @param sso      Incoming SSO payload
    * @param sig      Incoming SSO signature
    * @return         None if verified, Unauthorized otherwise
    */
  def VerifiedAction(username: String, sso: Option[String], sig: Option[String])
  = UserAction(username) andThen verifiedAction(sso, sig)

  private def verifiedAction(sso: Option[String], sig: Option[String]) = new ActionFilter[AuthRequest] {
    def filter[A](request: AuthRequest[A]): Future[Option[Result]] = Future.successful {
      if (sso.isEmpty || sig.isEmpty)
        Some(Unauthorized)
      else {
        Actions.this.sso.authenticate(sso.get, sig.get) match {
          case None =>
            Some(Unauthorized)
          case Some(spongeUser) =>
            if (spongeUser.username.equals(request.user.username))
              None
            else
              Some(Unauthorized)
        }
      }
    }
  }

  private def userAction(username: String) = new ActionFilter[AuthRequest] {
    def filter[A](request: AuthRequest[A]): Future[Option[Result]] = Future.successful {
      Actions.this.users.withName(username).flatMap[User] { toCheck =>
        val user = request.user
        if (user.equals(toCheck) || (toCheck.isOrganization && (user can EditSettings in toCheck.toOrganization)))
          Some(user)
        else
          None
      }.map[Option[Result]](user => None).getOrElse(Some(Unauthorized))
    }
  }

  private def authAction = new ActionRefiner[Request, AuthRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, AuthRequest[A]]] = Future.successful {
      users.current(request.session)
        .map(AuthRequest(_, request))
        .toRight(onUnauthorized(request))
    }
  }

  private def projectAction(author: String, slug: String) = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A])
    = Future.successful(maybeProjectRequest(request, Actions.this.projects.withSlug(author, slug)))
  }

  private def projectAction(pluginId: String) = new ActionRefiner[Request, ProjectRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, ProjectRequest[A]]]
    = Future.successful(maybeProjectRequest(request, Actions.this.projects.withPluginId(pluginId)))
  }

  private def maybeProjectRequest[A](request: Request[A], project: Option[Project]) = {
    project
      .flatMap(processProject(_, this.users.current(request.session)))
      .map(new ProjectRequest[A](_, request))
      .toRight(NotFound)
  }

  private def processProject(project: Project, user: Option[User]): Option[Project] = {
    if (project.isVisible || (user.isDefined && (user.get can HideProjects in GlobalScope)))
      Some(project)
    else
      None
  }

  private def authedProjectAction(author: String, slug: String)
  = new ActionRefiner[AuthRequest, AuthedProjectRequest] {
    def refine[A](request: AuthRequest[A]) = Future.successful {
      Actions.this.projects.withSlug(author, slug)
        .flatMap(processProject(_, Some(request.user)))
        .map(new AuthedProjectRequest[A](_, request))
        .toRight(NotFound)
    }
  }

  private def organizationAction(organization: String) = new ActionRefiner[Request, OrganizationRequest] {
    def refine[A](request: Request[A]): Future[Either[Result, OrganizationRequest[A]]] = Future.successful {
      Actions.this.organizations.withName(organization)
        .map(new OrganizationRequest(_, request))
        .toRight(NotFound)
    }
  }

  private def authedOrganizationAction(organization: String)
  = new ActionRefiner[AuthRequest, AuthedOrganizationRequest] {
    def refine[A](request: AuthRequest[A]) = Future.successful {
      Actions.this.organizations.withName(organization)
        .map(new AuthedOrganizationRequest[A](_, request))
        .toRight(NotFound)
    }
  }

}
