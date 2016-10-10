package controllers

import javax.inject.Inject

import controllers.routes.{Application => app, Users => self}
import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.access.UserBase.ORDERING_PROJECTS
import discourse.impl.OreDiscourseApi
import discourse.model.DiscourseUser
import form.OreForms
import models.user.{Notification, User}
import models.user.role.RoleModel
import ore.permission.EditSettings
import ore.user.notification.InviteFilters.InviteFilter
import ore.user.notification.NotificationFilters.NotificationFilter
import ore.user.notification.{InviteFilters, NotificationFilters}
import ore.user.{FakeUser, Prompts}
import ore.{OreConfig, OreEnv}
import play.api.i18n.MessagesApi
import play.api.mvc.{Security, _}
import security.pgp.PGPPublicKeyInfo
import views.{html => views}

/**
  * Controller for general user actions.
  */
class Users @Inject()(fakeUser: FakeUser,
                      forms: OreForms,
                      forums: OreDiscourseApi,
                      implicit override val messagesApi: MessagesApi,
                      implicit override val env: OreEnv,
                      implicit override val config: OreConfig,
                      implicit override val service: ModelService) extends BaseController {

  /**
    * Redirect to forums for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from forums
    * @param sig  Incoming signature from forums
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String], returnPath: Option[String]) = Action { implicit request =>
    val baseUrl = this.config.app.getString("baseUrl").get
    if (this.fakeUser.isEnabled) {
      // Log in as fake user (debug only)
      this.config.checkDebug()
      this.users.getOrCreate(this.fakeUser)
      this.redirectBack(returnPath.getOrElse(request.path), this.fakeUser.username)
    } else if (sso.isEmpty || sig.isEmpty) {
      // Check if forums are available and redirect to login if so
      if (this.forums.isAvailable)
        Redirect(this.forums.toForums(baseUrl + "/login")).flashing("url" -> returnPath.getOrElse(request.path))
      else
        Redirect(app.showHome(None, None, None, None))
          .flashing("error" -> this.messagesApi("error.noLogin"))
    } else {
      // Redirected from the forums, decode SSO payload received from forums and get Ore user
      val discourseUser: DiscourseUser = this.forums.authenticate(sso.get, sig.get)

      // Create the user on Ore if they don't exist
      this.users.getOrCreate(User.fromDiscourse(discourseUser)).refresh()

      // Finish authentication
      this.redirectBack(request.flash.get("url").getOrElse("/"), discourseUser.username)
    }
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut(returnPath: Option[String]) = Action { implicit request =>
    Redirect(this.config.app.getString("baseUrl").get + returnPath.getOrElse(request.path))
      .withNewSession.flashing("noRedirect" -> "true")
  }

  /**
    * Shows the User's [[models.project.Project]]s page for the user with the
    * specified username.
    *
    * @param username   Username to lookup
    * @return           View of user projects page
    */
  def showProjects(username: String, page: Option[Int]) = Action { implicit request =>
    val pageSize = this.config.users.getInt("project-page-size").get
    val p = page.getOrElse(1)
    val offset = (p - 1) * pageSize
    this.users.withName(username).map { u =>
      (u, u.projects.sorted(ordering = _.stars.desc, limit = pageSize, offset = offset))
    } map {
      case (user, projectSeq) => Ok(views.users.projects(user, projectSeq, p))
    } getOrElse {
      NotFound
    }
  }

  private def isThisUserOrOrganizationAdmin(toCheck: User, user: User)
  = user.equals(toCheck) || (toCheck.isOrganization && (user can EditSettings in toCheck.toOrganization))

  /**
    * Submits a change to the specified user's tagline.
    *
    * @param username   User to update
    * @return           View of user page
    */
  def saveTagline(username: String) = Authenticated { implicit request =>
    this.users.withName(username) match {
      case None =>
        NotFound
      case Some(user) =>
        if (isThisUserOrOrganizationAdmin(user, request.user)) {
          val tagline = this.forms.UserTagline.bindFromRequest.get.trim
          val maxLen = this.config.users.getInt("max-tagline-len").get
          if (tagline.length > maxLen) {
            Redirect(self.showProjects(user.username, None))
              .flashing("error" -> this.messagesApi("error.tagline.tooLong", maxLen))
          } else {
            user.tagline = tagline
            Redirect(self.showProjects(user.username, None))
          }
        } else
          Unauthorized
    }
  }

  def savePgpPublicKey(username: String) = Authenticated { implicit request =>
    this.users.withName(username) match {
      case None =>
        NotFound
      case Some(user) =>
        if (isThisUserOrOrganizationAdmin(user, request.user)) {
          val pubKey = this.forms.UserPgpPubKey.bindFromRequest.get
          println(PGPPublicKeyInfo.decode(pubKey))
          Ok
        } else
          Unauthorized
    }
  }

  /**
    * Shows a list of [[models.user.User]]s that have created a
    * [[models.project.Project]].
    */
  def showAuthors(sort: Option[String], page: Option[Int]) = Action { implicit request =>
    val ordering = sort.getOrElse(ORDERING_PROJECTS)
    val p = page.getOrElse(1)
    Ok(views.users.authors(this.users.getAuthors(ordering, p), ordering, p))
  }

  /**
    * Displays the current user's unread notifications.
    *
    * @return Unread notifications
    */
  def showNotifications(notificationFilter: Option[String], inviteFilter: Option[String]) = {
    Authenticated { implicit request =>
      val user = request.user

      // Get visible notifications
      val nFilter: NotificationFilter = notificationFilter
        .map(str => NotificationFilters.values
          .find(_.name.equalsIgnoreCase(str))
          .getOrElse(NotificationFilters.Unread))
        .getOrElse(NotificationFilters.Unread)
      val notifications: Seq[Notification] = nFilter(user.notifications)

      // Get visible invites
      val iFilter: InviteFilter = inviteFilter
        .map(str => InviteFilters.values
          .find(_.name.equalsIgnoreCase(str))
          .getOrElse(InviteFilters.All))
        .getOrElse(InviteFilters.All)
      val invites: Seq[RoleModel] = iFilter(user)

      Ok(views.users.notifications(
        notifications,
        invites,
        nFilter, iFilter
      ))
    }
  }

  /**
    * Marks a [[models.user.User]]'s notification as read.
    *
    * @param id Notification ID
    * @return   Ok if marked as read, NotFound if notification does not exist
    */
  def markNotificationRead(id: Int) = Authenticated { implicit request =>
    request.user.notifications.get(id) match {
      case None =>
        NotFound
      case Some(notification) =>
        notification.setRead(read = true)
        Ok
    }
  }

  /**
    * Marks a [[ore.user.Prompts.Prompt]] as read for the authenticated
    * [[models.user.User]].
    *
    * @param id Prompt ID
    * @return   Ok if successful
    */
  def markPromptRead(id: Int) = Authenticated { implicit request =>
    Prompts.values.find(_.id == id) match {
      case None =>
        BadRequest
      case Some(prompt) =>
        request.user.markPromptRead(prompt)
        Ok
    }
  }

  private def redirectBack(url: String, username: String)
  = Redirect(this.config.app.getString("baseUrl").get + url).withSession(Security.username -> username)

}
