package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Pages => self}
import ore.permission.EditPages
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import util.form.Forms
import views.html.projects.{pages => views}

/**
  * Controller for handling Page related actions.
  */
class Pages @Inject()(override val messagesApi: MessagesApi, ws: WSClient) extends BaseController(ws) {

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
    ProjectAction(author, slug, countView = true) { implicit request =>
      val project = request.project
      project.pages.withName(page) match {
        case None => NotFound
        case Some(p) => Ok(views.view(project, p))
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
      Ok(views.edit(project, page, project.getOrCreatePage(page).contents))
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
        hasErrors => Redirect(self.showEditor(author, slug, page)).flashing("error" -> hasErrors.errors.head.message),
        pageData => {
          request.project.getOrCreatePage(page).contents = pageData._2
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
      project.pages.remove(project.pages.withName(page).get)
      Redirect(routes.Projects.show(author, slug))
    }
  }

}
