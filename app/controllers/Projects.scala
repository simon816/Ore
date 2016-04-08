package controllers

import javax.inject.Inject

import controllers.routes.{Projects => self}
import models.project._
import play.api.i18n.MessagesApi
import play.api.mvc._
import plugin.{InvalidPluginFileException, ProjectManager}
import util.{Forms, Statistics}
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends BaseController {

  /**
    * Displays the "create project" page.
    *
    * @return Create project view
    */
  def showCreate = withAuth { context => implicit request =>
    Ok(views.projects.create(None))
  }

  /**
    * Uploads a Project's first plugin file for further processing.
    *
    * @return Result
    */
  def upload = { withUser(None, user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreate()).flashing("error" -> "No file submitted.")
      case Some(tmpFile) =>
        // Initialize plugin file
        ProjectManager.initUpload(tmpFile.ref, tmpFile.filename, user) match {
          case Failure(thrown) => if (thrown.isInstanceOf[InvalidPluginFileException]) {
            // PEBKAC
            Redirect(self.showCreate()).flashing("error" -> "Invalid plugin file.")
          } else {
            throw thrown
          }
          case Success(plugin) =>
            // Cache pending project for later use
            val meta = plugin.meta.get
            val project = Project.fromMeta(user.username, meta)
            Project.setPending(project, plugin)
            Redirect(self.showCreateWithMeta(project.ownerName, project.slug))
        }
    })
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author   Author of plugin
    * @param slug     Project slug
    * @return         Create project view
    */
  def showCreateWithMeta(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, slug) match {
      case None => Redirect(self.showCreate())
      case Some(pending) => Ok(views.projects.create(Some(pending)))
    })
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author   Author of project
    * @param slug     Project slug
    * @return         Redirection to project page if successful
    */
  def showFirstVersionCreate(author: String, slug: String) = { withUser(Some(author), user => implicit request => {
    Project.getPending(author, slug) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get)
        pendingProject.project.category = category
        val pendingVersion = pendingProject.initFirstVersion
        Redirect(routes.Versions.showCreateWithMeta(
          author, slug, pendingVersion.channelName, pendingVersion.version.versionString
        ))
    }})
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @return         View of project
    */
  def show(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => {
      Statistics.projectViewed(project, request)
      Ok(views.projects.pages.home(project, project.homePage))
    })
  }

  /**
    * Saves the specified Project from the settings manager.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of project
    */
  def save(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get)
      if (!category.equals(project.category)) {
        project.category = category
      }
      Redirect(self.show(author, slug))
    }))
  }

  /**
    * Sets the "starred" status of a Project for the current user.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param starred  True if should set to starred
    * @return         Result code
    */
  def setStarred(author: String, slug: String, starred: Boolean) = { withUser(None, user => implicit request =>
    withProject(author, slug, project => {
      val alreadyStarred = project.isStarredBy(user)
      if (starred) {
        if (!alreadyStarred) {
          project.starFor(user)
        }
      } else if (alreadyStarred) {
        project.unstarFor(user)
      }
      Ok
    }))
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @return         View of project
    */
  def showDiscussion(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => Ok(views.projects.discussion(project)))
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Project manager
    */
  def showManager(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => Ok(views.projects.manage(project))))
  }

  /**
    * Renames the specified project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Project homepage
    */
  def rename(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val newName = Project.sanitizeName(Forms.ProjectRename.bindFromRequest.get)
      if (!Project.isNamespaceAvailable(author, Project.slugify(newName))) {
        Redirect(self.showManager(author, slug)).flashing("error" -> "That name is not available.")
      } else {
        project.name = newName
        Redirect(self.show(author, project.slug))
      }
    }))
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Home page
    */
  def delete(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      project.delete.get
      Redirect(routes.Application.index(None))
        .flashing("success" -> ("Project \"" + project.name + "\" deleted."))
    }))
  }

  /**
    * Displays the documentation page editor for the specified project and page
    * name.
    *
    * @param author   Owner name
    * @param slug     Project slug
    * @param page     Page name
    * @return         Page editor
    */
  def showPageEdit(author: String, slug: String, page: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      Ok(views.projects.pages.edit(project, page, project.getOrCreatePage(page).contents))
    }))
  }

  /**
    * Saves changes made on a documentation page.
    *
    * @param author   Owner name
    * @param slug     Project slug
    * @param page     Page name
    * @return         Project home
    */
  def savePage(author: String, slug: String, page: String) = { withUser(Some(author), user => implicit request =>
    // TODO: Validate content size and title
    withProject(author, slug, project => {
      val pageForm = Forms.PageEdit.bindFromRequest.get
      project.getOrCreatePage(page).contents = pageForm._2
      Redirect(self.showPage(author, slug, page))
    }))
  }

  /**
    * Irreversibly deletes the specified Page from the specified Project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param page     Page name
    * @return         Redirect to Project homepage
    */
  def deletePage(author: String, slug: String, page: String) = withUser(Some(author), user => implicit request => {
    withProject(author, slug, project => {
      project.deletePage(page)
      Redirect(self.show(author, slug))
    })
  })

  /**
    * Displays the specified page.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @param page     Page name
    * @return         View of page
    */
  def showPage(author: String, slug: String, page: String) = Action { implicit request =>
    withProject(author, slug, project => {
      project.page(page) match {
        case None => NotFound
        case Some(p) => Ok(views.projects.pages.home(project, p))
      }
    })
  }

  /**
    * Displays a view of the specified Project's Channels.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         View of channels
    */
  def showChannels(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      Ok(views.projects.channels.list(project, project.channels))
    }))
  }

  /**
    * Creates a submitted channel for the specified Project.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Redirect to view of channels
    */
  def createChannel(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val channels = project.channels
      if (channels.size > Project.MAX_CHANNELS) {
        // Maximum reached
        Redirect(self.showChannels(author, slug))
          .flashing("error" -> "A project may only have up to five channels.")
      } else {
        val form = Forms.ChannelEdit.bindFromRequest.get
        val channelName = form._1.trim
        if (!Channel.isValidName(channelName)) {
          Redirect(self.showChannels(author, slug))
            .flashing("error" -> "Channel names must be between 1 and 15 and be alphanumeric.")
        } else {
          // Find submitted color
          ChannelColors.values.find(c => c.hex.equalsIgnoreCase(form._2)) match {
            case None => BadRequest("Invalid channel color.")
            case Some(color) => channels.find(c => c.color.equals(color)) match {
              case None => channels.find(c => c.name.equalsIgnoreCase(channelName)) match {
                case None =>
                  project.newChannel(channelName, color).get
                  Redirect(self.showChannels(author, slug))
                case Some(channel) =>
                  // Channel name taken
                  Redirect(self.showChannels(author, slug))
                    .flashing("error" -> "A channel with that name already exists.")
              }
              case Some(channel) =>
                // Channel color taken
                Redirect(self.showChannels(author, slug))
                  .flashing("error" -> "A channel with that color already exists.")
            }
          }
        }
      }
    }))
  }

  /**
    * Submits changes to an existing channel.
    *
    * @param author       Project owner
    * @param slug         Project slug
    * @param channelName  Channel name
    * @return             View of channels
    */
  def editChannel(author: String, slug: String, channelName: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, implicit project => {
      // Get all channels
      val form = Forms.ChannelEdit.bindFromRequest.get
      val newName = form._1.trim
      if (!Channel.isValidName(newName)) {
        Redirect(self.showChannels(author, slug))
          .flashing("error" -> "Channel names must be between 1 and 15 and be alphanumeric.")
      } else {
        // Find submitted channel by old name
        val channels = project.channels
        channels.find(c => c.name.equals(channelName)) match {
          case None => NotFound("Channel not found.")
          case Some(channel) => ChannelColors.values.find(c => c.hex.equalsIgnoreCase(form._2)) match {
            case None => BadRequest("Invalid channel color.")
            case Some(color) =>
              // Check if color is taken by different channel
              val colorChan = channels.find(c => c.color.equals(color))
              val colorTaken = colorChan.isDefined && !colorChan.get.equals(channel)

              // Check if name taken by different channel
              val nameChan = channels.find(c => c.name.equals(newName))
              val nameTaken = nameChan.isDefined && !nameChan.get.equals(channel)

              if (colorTaken) {
                Redirect(self.showChannels(author, slug))
                  .flashing("error" -> "A channel with that color already exists.")
              } else if (nameTaken) {
                Redirect(self.showChannels(author, slug))
                  .flashing("error" -> "A channel with that name already exists.")
              } else {
                // Change name if different
                if (!channelName.equals(newName)) {
                  channel.name = newName
                }

                // Change color if different
                if (!channel.color.equals(color)) {
                  channel.color = color
                }

                Redirect(self.showChannels(author, slug))
              }
          }
        }
      }
    }))
  }

  /**
    * Irreversibly deletes the specified channel and all version associated
    * with it.
    *
    * @param author       Project owner
    * @param slug         Project slug
    * @param channelName  Channel name
    * @return             View of channels
    */
  def deleteChannel(author: String, slug: String, channelName: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      val channels = project.channels
      if (channels.size == 1) {
        Redirect(self.showChannels(author, slug))
          .flashing("error" -> "You cannot delete your only channel.")
      } else {
        channels.find(c => c.name.equals(channelName)) match {
          case None => NotFound
          case Some(channel) =>
            if (channel.versions.nonEmpty && channels.count(c => c.versions.nonEmpty) == 1) {
              Redirect(self.showChannels(author, slug))
                .flashing("error" -> "You cannot delete your only non-empty channel.")
            } else {
              channel.delete(project).get
              Redirect(self.showChannels(author, slug))
            }
        }
      }
    }))
  }

}
