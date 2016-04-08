package controllers

import javax.inject.Inject

import controllers.routes.{Versions => self}
import models.project.Project.PendingProject
import models.project.{Channel, ChannelColors, Project, Version}
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import plugin.{InvalidPluginFileException, ProjectManager}
import util.{Forms, Statistics}
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * Controller for handling Version related actions.
  */
class Versions @Inject()(override val messagesApi: MessagesApi) extends BaseController {

  /**
    * Shows the specified version detail page.
    *
    * @param author         Owner name
    * @param slug           Project slug
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version detail view
    */
  def show(author: String, slug: String, channelName: String, versionString: String) = Action { implicit request =>
    withProject(author, slug, project => {
      project.channel(channelName) match {
        case None => NotFound
        case Some(channel) => channel.version(versionString) match {
          case None => NotFound
          case Some(version) => Ok(views.projects.versions.detail(project, channel, version))
        }
      }
    })
  }

  /**
    * Saves the specified Version's description.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param channelName    Version channel
    * @param versionString  Version name
    * @return               View of Version
    */
  def saveDescription(author: String, slug: String, channelName: String, versionString: String) = {
    withUser(Some(author), user => implicit request =>
      withProject(author, slug, project => {
        project.channel(channelName) match {
          case None => NotFound("Channel not found.")
          case Some(channel) => channel.version(versionString) match {
            case None => NotFound("Version not found.")
            case Some(version) =>
              val oldDesc = version.description
              val newDesc = Forms.VersionDescription.bindFromRequest.get.trim
              if ((oldDesc.isEmpty && !newDesc.isEmpty) || !oldDesc.get.equals(newDesc)) {
                version.description = newDesc
              }
              Redirect(self.show(author, slug, channelName, versionString))
          }
        }
      }))
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author     Owner of project
    * @param slug       Project slug
    * @param channels   Visible channels
    * @return           View of project
    */
  def showList(author: String, slug: String, channels: Option[String]) = Action { implicit request =>
    withProject(author, slug, project => {
      val allChannels = project.channels
      var channelNames = if (channels.isDefined) Some(channels.get.toLowerCase.split(",")) else None
      var visibleChannels = allChannels
      if (channelNames.isDefined) {
        visibleChannels = allChannels.filter(c => channelNames.get.contains(c.name.toLowerCase))
      }

      // Don't pass "visible channels" if all channels are visible
      val versions = if (allChannels.equals(visibleChannels)) project.versions else project.versions(visibleChannels)
      if (channelNames.isDefined && allChannels.map(_.name).toSet.subsetOf(channelNames.get.toSet)) {
        channelNames = None
      }

      Ok(views.projects.versions.list(project, allChannels, versions, channelNames))
    })
  }

  /**
    * Shows the creation form for new versions on projects.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @return         Version creation view
    */
  def showCreate(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, slug, project => {
      Ok(views.projects.versions.create(project, None, Some(project.channels), showFileControls = true))
    }))
  }

  /**
    * Uploads a new version for a project for further processing.
    *
    * @param author   Owner name
    * @param slug     Project slug
    * @return         Version create page (with meta)
    */
  def upload(author: String, slug: String) = { withUser(Some(author), user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showCreate(author, slug)).flashing("error" -> "Missing file")
      case Some(tmpFile) =>
        // Get project
        withProject(author, slug, project => {
          // Initialize plugin file
          ProjectManager.initUpload(tmpFile.ref, tmpFile.filename, user) match {
            case Failure(thrown) => if (thrown.isInstanceOf[InvalidPluginFileException]) {
              // PEBKAC
              Redirect(self.showCreate(author, slug))
                .flashing("error" -> "Invalid plugin file.")
            } else {
              throw thrown
            }
            case Success(plugin) =>
              val meta = plugin.meta.get
              if (!meta.getId.equals(project.pluginId)) {
                Redirect(self.showCreate(author, slug))
                  .flashing("error" -> "The uploaded plugin ID must match your project's plugin ID.")
              } else {
                // Create version from meta file
                val version = Version.fromMeta(project, meta)

                // Get first channel for default
                val channelName: String = project.channels.head.name

                // Cache for later use
                Version.setPending(author, slug, channelName, version, plugin)
                Redirect(self.showCreateWithMeta(author, slug, channelName, version.versionString))
              }
          }
        })
    })
  }

  /**
    * Displays the "version create" page with the associated plugin meta-data.
    *
    * @param author         Owner name
    * @param slug           Project slug
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version create view
    */
  def showCreateWithMeta(author: String, slug: String,
                                channelName: String, versionString: String) = {
    withUser(Some(author), user => implicit request =>
      // Get pending version
      Version.getPending(author, slug, channelName, versionString) match {
        case None => Redirect(self.showCreate(author, slug))
        case Some(pendingVersion) =>
          // Get project
          pendingOrReal(author, slug) match {
            case None => Redirect(self.showCreate(author, slug))
            case Some(p) => p match {
              case pending: PendingProject =>
                Ok(views.projects.versions.create(
                  pending.project, Some(pendingVersion), None, showFileControls = false
                ))
              case real: Project =>
                Ok(views.projects.versions.create(
                  real, Some(pendingVersion), Some(real.channels), showFileControls = true
                ))
            }
          }
      })
  }

  /**
    * Completes the creation of the specified pending version or project if
    * first version.
    *
    * @param author         Owner name
    * @param slug           Project slug
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               New version view
    */
  def create(author: String, slug: String, channelName: String, versionString: String) = {
    withUser(Some(author), user => implicit request => {
      Version.getPending(author, slug, channelName, versionString) match {
        case None => Redirect(self.showCreate(author, slug))
        case Some(pendingVersion) =>
          // Gather form data
          val form = Forms.ChannelEdit.bindFromRequest.get
          val submittedName = form._1.trim
          pendingVersion.channelName = submittedName
          ChannelColors.values.find(color => color.hex.equalsIgnoreCase(form._2)) match {
            case None => BadRequest("Invalid channel color.")
            case Some(color) => pendingVersion.channelColor = color
          }

          // Validate channel name
          if (!Channel.isValidName(submittedName)) {
            Redirect(routes.Application.index(None))
              .flashing("error" -> "Channel names must be between 1 and 15 and be alphanumeric.")
          } else {
            // Check for pending project
            Project.getPending(author, slug) match {
              case None =>
                // No pending project, just create the version for the existing project
                withProject(author, slug, project => {
                  // Check if creating a new channel
                  val existingChannel: Channel = project.channel(submittedName).orNull

                  // Check if color is available
                  var colorTaken: Boolean = false
                  if (existingChannel == null) {
                    colorTaken = project.channel(pendingVersion.channelColor).isDefined
                  }

                  if (colorTaken) {
                    pendingVersion.cache()
                    Redirect(self.showCreateWithMeta(author, slug, channelName, versionString))
                      .flashing("error" -> "A channel with that color already exists.")
                  } else {
                    // Check for existing version
                    var existingVersion: Version = null
                    if (existingChannel != null) {
                      existingVersion = existingChannel.version(versionString).orNull
                    }

                    if (existingVersion != null) {
                      Redirect(self.showCreateWithMeta(author, slug, channelName, versionString))
                        .flashing("error" -> "Version already exists.")
                    } else {
                      pendingVersion.complete.get
                      Redirect(self.show(author, slug, submittedName, versionString))
                    }
                  }
                })
              case Some(pendingProject) =>
                // Found a pending project, create the project and it's first version
                pendingProject.complete.get
                Redirect(routes.Projects.show(author, slug))
            }
          }
      }
    })
  }

  /**
    * Deletes the specified version and returns to the version page.
    *
    * @param author         Owner name
    * @param slug           Project slug
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Versions page
    */
  def delete(author: String, slug: String, channelName: String, versionString: String) = {
    withUser(Some(author), user => implicit request =>
      withProject(author, slug, project => {
        project.channel(channelName) match {
          case None => NotFound("Channel not found.")
          case Some(channel) => channel.version(versionString) match {
            case None => NotFound("Version not found.")
            case Some(version) =>
              channel.deleteVersion(version, project).get
              Redirect(self.showList(author, slug, None))
          }
        }
      }))
  }

  /**
    * Sends the specified Project Version to the client.
    *
    * @param author         Project owner
    * @param name           Project name
    * @param channelName    Version channel
    * @param versionString  Version string
    * @return               Sent file
    */
  def download(author: String, name: String, channelName: String, versionString: String) = Action { implicit request =>
    withProject(author, name, project => {
      project.channel(channelName) match {
        case None => NotFound("Channel not found.")
        case Some(channel) => channel.version(versionString) match {
          case None => NotFound("Version not found.")
          case Some(version) =>
            Statistics.versionDownloaded(project, version, request)
            Ok.sendFile(ProjectManager.uploadPath(author, name, versionString, channelName).toFile)
        }
      }
    })
  }

  /**
    * Sends the specified project's current recommended version to the client.
    *
    * @param author   Project owner
    * @param slug     Project slug
    * @return         Sent file
    */
  def downloadRecommended(author: String, slug: String) = Action { implicit request =>
    withProject(author, slug, project => {
      val rv = project.recommendedVersion
      Statistics.versionDownloaded(project, rv, request)
      Ok.sendFile(ProjectManager.uploadPath(author, project.name, rv.versionString, rv.channel.name).toFile)
    })
  }

  private def pendingOrReal(author: String, slug: String): Option[Any] = {
    // Returns either a PendingProject or existing Project
    Project.withSlug(author, slug) match {
      case None => Project.getPending(author, slug)
      case Some(project) => Some(project)
    }
  }

}
