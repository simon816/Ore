package controllers.project

import javax.inject.Inject

import controllers.BaseController
import controllers.Requests.AuthRequest
import db.ModelService
import form.OreForms
import models.competition.Competition
import ore.OreConfig
import ore.permission.EditCompetitions
import org.spongepowered.play.security.SingleSignOnConsumer
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import util.StringUtils
import views.{html => views}

/**
  * Handles competition based actions.
  */
class Competitions @Inject()(implicit override val messagesApi: MessagesApi,
                             override val config: OreConfig,
                             override val service: ModelService,
                             override val sso: SingleSignOnConsumer,
                             forms: OreForms)
                             extends BaseController {

  private val self = routes.Competitions

  private def EditCompetitionsAction = Authenticated andThen PermissionAction[AuthRequest](EditCompetitions)

  /**
    * Shows the competition administrative panel.
    *
    * @return Competition manager
    */
  def showManager() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.manage())
  }

  /**
    * Shows the competition creator.
    *
    * @return Competition creator
    */
  def showCreator() = EditCompetitionsAction { implicit request =>
    Ok(views.projects.competitions.create())
  }

  /**
    * Creates a new competition.
    *
    * @return Redirect to manager or creator with errors.
    */
  def create() = EditCompetitionsAction { implicit request =>
    this.forms.CompetitionCreate.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showCreator(), hasErrors),
      formData => {
        if (!formData.checkDates())
          Redirect(self.showCreator()).withError("error.dates.competition")
        else if (this.competitions.exists(StringUtils.equalsIgnoreCase(_.name, formData.name)))
          Redirect(self.showCreator()).withError("error.unique.competition.name")
        else {
          this.competitions.add(new Competition(request.user, formData))
          Redirect(self.showManager())
        }
      }
    )
  }

  /**
    * Saves the competition with the specified ID.
    *
    * @param id Competition ID
    * @return   Redirect to manager
    */
  def save(id: Int) = (EditCompetitionsAction andThen AuthedCompetitionAction(id)) { implicit request =>
    println(request.body.asFormUrlEncoded)
    this.forms.CompetitionSave.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showManager(), hasErrors),
      formData => {
        if (!formData.checkDates())
          Redirect(self.showManager()).withError("error.dates.competition")
        else {
          request.competition.save(formData)
          Redirect(self.showManager()).withSuccess("success.saved.competition")
        }
      }
    )
  }

  /**
    * Deletes the competition with the specified ID.
    *
    * @param id Competition ID
    * @return   Redirect to manager
    */
  def delete(id: Int) = (EditCompetitionsAction andThen AuthedCompetitionAction(id)) { implicit request =>
    this.competitions.remove(request.competition)
    Redirect(self.showManager()).withSuccess("success.deleted.competition")
  }

  /**
    * Sets the specified competition's banner image.
    *
    * @param id Competition ID
    * @return   Json response
    */
  def setBanner(id: Int) = (EditCompetitionsAction andThen AuthedCompetitionAction(id)) { implicit request =>
    request.body.asMultipartFormData.get.file("banner") match {
      case None =>
        Ok(Json.obj("error" -> this.messagesApi("error.noFile")))
      case Some(file) =>
        this.competitions.saveBanner(request.competition, file.ref.file, file.filename)
        Ok(Json.obj("bannerUrl" -> self.showBanner(id).path()))
    }
  }

  /**
    * Displays the specified competition's banner image, if any, NotFound
    * otherwise.
    *
    * @param id Competition ID
    * @return   Banner image
    */
  def showBanner(id: Int) = CompetitionAction(id) { implicit request =>
    this.competitions.getBannerPath(request.competition).map(showImage).getOrElse(NotFound)
  }

  /**
    * Displays the project entries in the specified competition.
    *
    * @param id Competition ID
    * @return   List of project entries
    */
  def showProjects(id: Int, page: Option[Int]) = CompetitionAction(id) { implicit request =>
    val pageSize = this.config.projects.getInt("init-load").get
    Ok(views.projects.competitions.projects(request.competition, page.getOrElse(1), pageSize))
  }

  /**
    * Submits a project to the specified competition.
    *
    * @param id Competition ID
    * @return   Redirect to project list
    */
  def submitProject(id: Int) = AuthedCompetitionAction(id) { implicit request =>
    this.forms.CompetitionSubmitProject.bindFromRequest().fold(
      hasErrors =>
        FormError(self.showProjects(id, None), hasErrors),
      projectId =>
        request.user.projects.get(projectId) match {
          case None =>
            Redirect(self.showProjects(id, None)).withError("error.competition.submit.invalidProject")
          case Some(project) =>
            this.competitions.submitProject(project, request.competition) match {
              case None =>
                Redirect(self.showProjects(id, None))
              case Some(error) =>
                Redirect(self.showProjects(id, None)).withError(error)
            }
        }
    )
  }

}
