package controllers

import javax.inject.Inject

import controllers.routes.{Application => app, Users => self}
import form.Forms
import forums.SpongeForums._
import models.user.{FakeUser, User}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.{Security, _}
import util.C._
import views.{html => views}

class Users @Inject()(override val messagesApi: MessagesApi, implicit val ws: WSClient) extends BaseController {

  /**
    * Redirect to forums for SSO authentication and then back here again.
    *
    * @param sso  Incoming payload from forums
    * @param sig  Incoming signature from forums
    * @return     Logged in home
    */
  def logIn(sso: Option[String], sig: Option[String], returnPath: Option[String]) = Action { implicit request =>
    val baseUrl = AppConf.getString("baseUrl").get
    if (FakeUser.IsEnabled) {
      User.getOrCreate(FakeUser)
      redirectBack(returnPath.getOrElse(request.path), FakeUser.username)
    } else if (sso.isEmpty || sig.isEmpty) {
      Redirect(Auth.getRedirect(baseUrl + "/login")).flashing("url" -> returnPath.getOrElse(request.path))
    } else {
      // Decode SSO payload and get Ore user
      val user = Auth.authenticate(sso.get, sig.get)
      redirectBack(request2flash.get("url").get, user.username)
    }
  }

  private def redirectBack(url: String, username: String) = {
    Redirect(AppConf.getString("baseUrl").get + url).withSession(Security.username -> username)
  }

  /**
    * Clears the current session.
    *
    * @return Home page
    */
  def logOut(returnPath: Option[String]) = Action { implicit request =>
    Redirect(AppConf.getString("baseUrl").get + returnPath.getOrElse(request.path)).withNewSession.flashing("noRedirect" -> "true")
  }

  /**
    * Shows the User page for the user with the specified username.
    *
    * @param username   Username to lookup
    * @return           View of user page
    */
  def show(username: String) = Action { implicit request =>
    User.withName(username) match {
      case None => NotFound
      case Some(user) => Ok(views.user(user))
    }
  }

  /**
    * Submits a change to the specified user's tagline.
    *
    * @param username   User to update
    * @return           View of user page
    */
  def saveTagline(username: String) = Authenticated { implicit request =>
    val user = request.user
    val tagline = Forms.UserTagline.bindFromRequest.get.trim
    if (tagline.length > User.MaxTaglineLength) {
      Redirect(self.show(user.username)).flashing("error" -> "Tagline is too long.")
    } else {
      user.tagline = tagline
      Redirect(self.show(user.username))
    }
  }

}
