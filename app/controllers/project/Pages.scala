package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Pages => self}
import db.OrePostgresDriver.api._
import form.Forms
import ore.Statistics
import ore.permission.EditPages
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import views.html.projects.{pages => views}

/**
  * Controller for handling Page related actions.
  */
class Pages @Inject()(override val messagesApi: MessagesApi, implicit val ws: WSClient) extends BaseController {

  private def PageEditAction(author: String, slug: String) = {
    AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditPages)
  }

  /**
    * Displays the specified page.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @param page   Page name
    * @return View of page
    */
  def show(author: String, slug: String, page: String) = {
    ProjectAction(author, slug) { implicit request =>
      val project = request.project
      project.pages.find(_.name === page) match {
        case None => NotFound
        case Some(p) => Statistics.projectViewed(implicit request => Ok(views.view(project, p)))
      }
    }
  }

  /**
    * Displays the documentation page editor for the specified project and page
    * name.
    *
    * @param author Owner name
    * @param slug   Project slug
    * @param page   Page name
    * @return Page editor
    */
  def showEditor(author: String, slug: String, page: String) = {
    PageEditAction(author, slug) { implicit request =>
      val project = request.project
      Ok(views.view(project, project.getOrCreatePage(page), editorOpen = true))
    }
  }

  /**
    * Saves changes made on a documentation page.
    *
    * @param author Owner name
    * @param slug   Project slug
    * @param page   Page name
    * @return Project home
    */
  def save(author: String, slug: String, page: String) = {
    PageEditAction(author, slug) { implicit request =>
      Forms.PageEdit.bindFromRequest.fold(
        hasErrors => Redirect(self.show(author, slug, page)).flashing("error" -> hasErrors.errors.head.message),
        pageData => {
          request.project.getOrCreatePage(page).contents = pageData
          Redirect(self.show(author, slug, page))
        }
      )
    }
  }

  /**
    * Irreversibly deletes the specified Page from the specified Project.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @param page   Page name
    * @return Redirect to Project homepage
    */
  def delete(author: String, slug: String, page: String) = {
    PageEditAction(author, slug) { implicit request =>
      val project = request.project
      project.pages.remove(project.pages.find(_.name === page).get)
      Redirect(routes.Projects.show(author, slug))
    }
  }

}
