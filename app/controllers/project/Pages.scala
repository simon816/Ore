package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Pages => self}
import db.ModelService
import form.OreForms
import forums.DiscourseApi
import models.project.Page
import ore.permission.EditPages
import ore.statistic.StatTracker
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSClient
import play.api.mvc.Action
import util.OreConfig
import util.StringUtils.equalsIgnoreCase
import views.html.projects.{pages => views}

/**
  * Controller for handling Page related actions.
  */
class Pages @Inject()(override val messagesApi: MessagesApi,
                      val forms: OreForms,
                      val stats: StatTracker,
                      implicit val config: OreConfig,
                      implicit val ws: WSClient,
                      implicit override val forums: DiscourseApi,
                      implicit override val service: ModelService) extends BaseController {

  private def PageEditAction(author: String, slug: String)
  = AuthedProjectAction(author, slug) andThen ProjectPermissionAction(EditPages)

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
      project.pages.find(equalsIgnoreCase(_.name, page)) match {
        case None => NotFound
        case Some(p) => stats.projectViewed(implicit request => Ok(views.view(project, p)))
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
    * Renders the submitted page content and returns the result.
    *
    * @return Rendered content
    */
  def showPreview = Action { implicit request =>
    Ok(Page.MarkdownProcessor.markdownToHtml((request.body.asJson.get \ "raw").as[String]))
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
      forms.PageEdit.bindFromRequest.fold(
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
      project.pages.remove(project.pages.find(equalsIgnoreCase(_.name, page)).get)
      Redirect(routes.Projects.show(author, slug))
    }
  }

}
