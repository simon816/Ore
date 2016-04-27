package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Channels => self}
import form.Forms
import ore.permission.EditChannels
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import views.html.projects.{channels => views}

/**
  * Controller for handling Channel related actions.
  */
class Channels @Inject()(override val messagesApi: MessagesApi, implicit val ws: WSClient) extends BaseController {

  private def ChannelEditAction(author: String, slug: String) = {
    AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditChannels)
  }

  /**
    * Displays a view of the specified Project's Channels.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return View of channels
    */
  def showList(author: String, slug: String) = {
    ChannelEditAction(author, slug) { implicit request =>
      val project = request.project
      Ok(views.list(project, project.channels.seq))
    }
  }

  /**
    * Creates a submitted channel for the specified Project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Redirect to view of channels
    */
  def create(author: String, slug: String) = {
    ChannelEditAction(author, slug) { implicit request =>
      Forms.ChannelEdit.bindFromRequest.fold(
        hasErrors => Redirect(self.showList(author, slug)).flashing("error" -> hasErrors.errors.head.message),
        channelData => channelData.addTo(request.project).fold(
          error => Redirect(self.showList(author, slug)).flashing("error" -> error),
          channel => Redirect(self.showList(author, slug))
        )
      )
    }
  }

  /**
    * Submits changes to an existing channel.
    *
    * @param author      Project owner
    * @param slug        Project slug
    * @param channelName Channel name
    * @return View of channels
    */
  def save(author: String, slug: String, channelName: String) = {
    ChannelEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      Forms.ChannelEdit.bindFromRequest.fold(
        hasErrors => Redirect(self.showList(author, slug)).flashing("error" -> hasErrors.errors.head.message),
        channelData => channelData.saveTo(channelName).map { error =>
          Redirect(self.showList(author, slug)).flashing("error" -> error)
        } getOrElse {
          Redirect(self.showList(author, slug))
        }
      )
    }
  }

  /**
    * Irreversibly deletes the specified channel and all version associated
    * with it.
    *
    * @param author      Project owner
    * @param slug        Project slug
    * @param channelName Channel name
    * @return View of channels
    */
  def delete(author: String, slug: String, channelName: String) = {
    ChannelEditAction(author, slug) { implicit request =>
      val project = request.project
      val channels = project.channels.values
      if (channels.size == 1) {
        Redirect(self.showList(author, slug))
          .flashing("error" -> "You cannot delete your only channel.")
      } else {
        channels.find(c => c.name.equals(channelName)) match {
          case None => NotFound
          case Some(channel) =>
            if (channel.versions.nonEmpty && channels.count(c => c.versions.nonEmpty) == 1) {
              Redirect(self.showList(author, slug))
                .flashing("error" -> "You cannot delete your only non-empty channel.")
            } else {
              channel.delete(project)
              Redirect(self.showList(author, slug))
            }
        }
      }
    }
  }

}
