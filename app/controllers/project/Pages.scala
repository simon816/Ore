package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.project.routes.{Pages => self}
import db.ModelService
import form.OreForms
import discourse.DiscourseApi
import discourse.impl.OreDiscourseApi
import models.project.Page
import ore.permission.EditPages
import ore.{OreConfig, OreEnv, StatTracker}
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import security.sso.SingleSignOn
import views.html.projects.{pages => views}
import util.StringUtils._

/**
  * Controller for handling Page related actions.
  */
class Pages @Inject()(forms: OreForms,
                      stats: StatTracker,
                      implicit override val sso: SingleSignOn,
                      implicit override val messagesApi: MessagesApi,
                      implicit override val env: OreEnv,
                      implicit override val config: OreConfig,
                      implicit override val service: ModelService)
                      extends BaseController {

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
        case Some(p) => this.stats.projectViewed(implicit request => Ok(views.view(project, p)))
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
  def showPreview() = Action { implicit request =>
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
      this.forms.PageEdit.bindFromRequest.fold(
        hasErrors =>
          Redirect(self.show(author, slug, page)).flashing("error" -> hasErrors.errors.head.message),
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
