package controllers

import javax.inject.Inject

import controllers.routes.{Projects => self}
import db.Storage
import models.project.Project.PendingProject
import models.project._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import plugin.{InvalidPluginFileException, Pages, ProjectManager}
import util.{Forms, Statistics}
import views.{html => views}

import scala.util.{Failure, Success}

/**
  * TODO: Replace NotFounds, BadRequests, etc with pretty views
  * TODO: Localize
  */
class Projects @Inject()(override val messagesApi: MessagesApi) extends Controller with I18nSupport with Secured {

  private def withProject(author: String, name: String, f: Project => Result): Result = {
    Storage.now(Storage.getProject(author, name)) match {
      case Failure(thrown) => NotFound
      case Success(project) => f(project)
    }
  }

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
            val meta = plugin.getMeta.get
            val project = Project.fromMeta(user.username, meta)
            Project.setPending(project, plugin)
            Redirect(self.showCreateWithMeta(project.owner, project.getName))
        }
    })
  }

  /**
    * Displays the "create project" page with uploaded plugin meta data.
    *
    * @param author   Author of plugin
    * @param name     Name of plugin
    * @return         Create project view
    */
  def showCreateWithMeta(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => Redirect(self.showCreate())
      case Some(pending) => Ok(views.projects.create(Some(pending)))
    })
  }

  /**
    * Continues on to the second step of Project creation where the user
    * publishes their Project.
    *
    * @param author   Author of project
    * @param name     Name of project
    * @return         Redirection to project page if successful
    */
  def showFirstVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    Project.getPending(author, name) match {
      case None => BadRequest("No project to create.")
      case Some(pendingProject) =>
        val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get)
        pendingProject.project.setCategory(category)
        val pendingVersion = pendingProject.initFirstVersion
        Redirect(self.showVersionCreateWithMeta(
          author, name, pendingVersion.getChannelName, pendingVersion.version.versionString
        ))
    })
  }

  /**
    * Displays the Project with the specified author and name.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         View of project
    */
  def show(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => {
      Statistics.projectViewed(project, request)
      Ok(views.projects.pages.home(project, Pages.getHome(author, name)))
    })
  }

  /**
    * Saves the specified Project from the settings manager.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         View of project
    */
  def save(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      val category = Categories.withName(Forms.ProjectCategory.bindFromRequest.get)
      if (!category.equals(project.getCategory)) {
        project.setCategory(category)
      }
      Redirect(self.show(author, name))
    }))
  }

  /**
    * Sets the "starred" status of a Project for the current user.
    *
    * @param author   Project owner
    * @param name     Project name
    * @param starred  True if should set to starred
    * @return         Result code
    */
  def setStarred(author: String, name: String, starred: Boolean) = { withUser(None, user => implicit request =>
    withProject(author, name, project => {
      val alreadyStarred = project.isStarredBy(user)
      if (starred) {
        if (!alreadyStarred) {
          Storage.now(project.starFor(user)) match {
            case Failure(thrown) => throw thrown
            case Success(i) => ;
          }
        }
      } else if (alreadyStarred) {
        Storage.now(project.unstarFor(user)) match {
          case Failure(thrown) => throw thrown
          case Success(i) => ;
        }
      }
      Ok
    }))
  }

  /**
    * Displays the documentation page editor for the specified project and page
    * name.
    *
    * @param author   Owner name
    * @param name     Project name
    * @param page     Page name
    * @return         Page editor
    */
  def showPageEdit(author: String, name: String, page: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      Ok(views.projects.pages.edit(project, page, Pages.getOrCreate(project, page).getContents))
    }))
  }

  /**
    * Saves changes made on a documentation page.
    *
    * @param author   Owner name
    * @param name     Project name
    * @param page     Page name
    * @return         Project home
    */
  def savePage(author: String, name: String, page: String) = { withUser(Some(author), user => implicit request =>
    // TODO: Validate content size and title
    withProject(author, name, project => {
      val pageForm = Forms.PageEdit.bindFromRequest.get
      Pages.getOrCreate(project, page).update(pageForm._1, pageForm._2)
      Redirect(self.showPage(author, name, page))
    }))
  }

  /**
    * Irreversibly deletes the specified Page from the specified Project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @param page     Page name
    * @return         Redirect to Project homepage
    */
  def deletePage(author: String, name: String, page: String) = withUser(Some(author), user => implicit request => {
    Pages.delete(author, name, page)
    Redirect(self.show(author, name))
  })

  /**
    * Displays the specified page.
    *
    * @param author   Project owner
    * @param name     Project name
    * @param page     Page name
    * @return         View of page
    */
  def showPage(author: String, name: String, page: String) = Action { implicit request =>
    withProject(author, name, project => {
      Pages.get(author, name, page) match {
        case None => NotFound
        case Some(p) => Ok(views.projects.pages.home(project, p))
      }
    })
  }

  /**
    * Shows the specified version detail page.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version detail view
    */
  def showVersion(author: String, name: String, channelName: String, versionString: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound
          case Some(channel) => Storage.now(channel.getVersion(versionString)) match {
            case Failure(thrown) => throw thrown
            case Success(versionOpt) => versionOpt match {
              case None => NotFound
              case Some(version) => Ok(views.projects.versions.detail(project, channel, version))
            }
          }
        }
      }
    })
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author     Owner of project
    * @param name       Name of project
    * @param channels   Visible channels
    * @return           View of project
    */
  def showVersions(author: String, name: String, channels: Option[String]) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(chans) =>
          var channelNames = if (channels.isDefined) Some(channels.get.split(",")) else None
          var visibleChannels = chans
          if (channelNames.isDefined) {
            visibleChannels = chans.filter(c => channelNames.get.contains(c.getName))
          }

          // Don't pass "visible channels" if all channels are visible
          val versions = if (chans.equals(visibleChannels)) project.getVersions else project.getVersions(visibleChannels)
          if (channelNames.isDefined && chans.map(_.getName).toSet.subsetOf(channelNames.get.toSet)) {
            channelNames = None
          }

          Ok(views.projects.versions.list(project, chans, versions, channelNames));
      }
    })
  }

  /**
    * Shows the creation form for new versions on projects.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         Version creation view
    */
  def showVersionCreate(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) =>
          Ok(views.projects.versions.create(
            project, None, Some(channels), showFileControls = true
          ))
      }
    }))
  }

  /**
    * Uploads a new version for a project for further processing.
    *
    * @param author   Owner name
    * @param name     Project name
    * @return         Version create page (with meta)
    */
  def uploadVersion(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    request.body.asMultipartFormData.get.file("pluginFile") match {
      case None => Redirect(self.showVersionCreate(author, name)).flashing("error" -> "Missing file")
      case Some(tmpFile) =>
        // Get project
        withProject(author, name, project => {
          // Initialize plugin file
          ProjectManager.initUpload(tmpFile.ref, tmpFile.filename, user) match {
            case Failure(thrown) => if (thrown.isInstanceOf[InvalidPluginFileException]) {
              // PEBKAC
              Redirect(self.showVersionCreate(author, name))
                .flashing("error" -> "Invalid plugin file.")
            } else {
              throw thrown
            }
            case Success(plugin) =>
              // Cache version for later use
              val meta = plugin.getMeta.get
              val version = Version.fromMeta(project, meta)
              val channelName = Channel.getSuggestedNameForVersion(version.versionString)
              Version.setPending(author, name, channelName, version, plugin)
              Redirect(self.showVersionCreateWithMeta(author, name, channelName, version.versionString))
          }
        })
    })
  }

  private def pendingOrReal(author: String, name: String): Option[Any] = {
    // Returns either a PendingProject or existing Project
    Storage.now(Storage.optProject(author, name)) match {
      case Failure(thrown) => throw thrown
      case Success(projectOpt) => projectOpt match {
        case None => Project.getPending(author, name)
        case Some(project) => Some(project)
      }
    }
  }

  /**
    * Displays the "version create" page with the associated plugin meta-data.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Version create view
    */
  def showVersionCreateWithMeta(author: String, name: String,
                                channelName: String, versionString: String) = {
                                withUser(Some(author), user => implicit request =>
    // Get pending version
    Version.getPending(author, name, channelName, versionString) match {
      case None => Redirect(routes.Application.index(None))
      case Some(pendingVersion) =>
        // Get project
        pendingOrReal(author, name) match {
          case None => Redirect(routes.Application.index(None))
          case Some(p) => p match {
            case pending: PendingProject =>
              Ok(views.projects.versions.create(
                pending.project, Some(pendingVersion), None, showFileControls = false
              ))
            case real: Project =>
              Storage.now(real.getChannels) match {
                case Failure(thrown) => throw thrown
                case Success(channels) =>
                  Ok(views.projects.versions.create(
                    real, Some(pendingVersion), Some(channels), showFileControls = true
                  ))
              }
          }
        }
    })
  }

  /**
    * Completes the creation of the specified pending version or project if
    * first version.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               New version view
    */
  def createVersion(author: String, name: String, channelName: String, versionString: String) = {
                    withUser(Some(author), user => implicit request =>
    Version.getPending(author, name, channelName, versionString) match {
      case None => BadRequest("No version to create.")
      case Some(pendingVersion) =>
        // Check for pending project
        Project.getPending(author, name) match {
          case None => pendingVersion.complete match {
            case Failure(thrown) => throw thrown
            case Success(void) =>
              // No pending project, just create the version for the existing project
              Redirect(self.showVersion(author, name, channelName, versionString))
          }
          case Some(pendingProject) =>
            // Found a pending project, create the project and it's first version
            val form = Forms.ChannelNewProject.bindFromRequest.get
            ChannelColors.values.find(color => color.hex.equalsIgnoreCase(form._2)) match {
              case None => BadRequest("Invalid channel color.")
              case Some(color) => pendingVersion.setChannelColor(color)
            }
            pendingVersion.setChannelName(form._1)

            pendingProject.complete match {
              case Failure(thrown) => throw thrown
              case Success(void) => Redirect(self.show(author, name))
          }
      }
    })
  }

  /**
    * Deletes the specified version and returns to the version page.
    *
    * @param author         Owner name
    * @param name           Project name
    * @param channelName    Channel name
    * @param versionString  Version name
    * @return               Versions page
    */
  def deleteVersion(author: String, name: String, channelName: String, versionString: String) = {
                    withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound("Channel not found.")
          case Some(channel) => Storage.now(channel.getVersion(versionString)) match {
            case Failure(thrown) => throw thrown
            case Success(versionOpt) => versionOpt match {
              case None => NotFound("Version not found.")
              case Some(version) => channel.deleteVersion(version, project) match {
                case Failure(thrown) => throw thrown
                case Success(void) => Redirect(self.showVersions(author, name, None))
              }
            }
          }
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
  def downloadVersion(author: String, name: String, channelName: String, versionString: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannel(channelName)) match {
        case Failure(thrown) => throw thrown
        case Success(channelOpt) => channelOpt match {
          case None => NotFound("Channel not found.")
          case Some(channel) =>
            Storage.now(channel.getVersion(versionString)) match {
              case Failure(thrown) => throw thrown
              case Success(versionOpt) => versionOpt match {
                case None => NotFound("Version not found.")
                case Some(version) =>
                  Statistics.versionDownloaded(project, version, request)
                  Ok.sendFile(ProjectManager.getUploadPath(author, name, versionString, channelName).toFile)
              }
            }
        }
      }
    })
  }

  /**
    * Sends the specified project's current recommended version to the client.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Sent file
    */
  def downloadRecommendedVersion(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getRecommendedVersion) match {
        case Failure(thrown) => throw thrown
        case Success(version) =>
          Storage.now(version.getChannel) match {
            case Failure(thrown) => throw thrown
            case Success(channel) =>
              Statistics.versionDownloaded(project, version, request)
              Ok.sendFile(ProjectManager.getUploadPath(author, name, version.versionString, channel.getName).toFile)
          }
      }
    })
  }

  /**
    * Displays a view of the specified Project's Channels.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         View of channels
    */
  def showChannels(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Storage.now(project.getChannels) match {
      case Failure(thrown) => throw thrown;
      case Success(channels) => Ok(views.projects.channels.list(project, channels))
    }))
  }

  /**
    * Creates a submitted channel for the specified Project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Redirect to view of channels
    */
  def createChannel(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      // Get all channels
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) => if (channels.size > Channel.MAX_AMOUNT) {
          // Maximum reached
          Redirect(self.showChannels(author, name))
            .flashing("error" -> "A project may only have up to five channels.")
        } else {
          val form = Forms.ChannelEdit.bindFromRequest.get
          val channelName = form._1
          // Find submitted color
          ChannelColors.values.find(color => color.hex.equalsIgnoreCase(form._2)) match {
            case None => BadRequest("Invalid channel color.")
            case Some(color) => channels.find(c => c.getColor.equals(color)) match {
              case None => channels.find(c => c.getName.equals(channelName)) match {
                case None => project.newChannel(channelName, color) match {
                  case Failure(thrown) => throw thrown
                  case Success(channel) => Redirect(self.showChannels(author, name))
                }
                case Some(channel) =>
                  // Channel name taken
                  Redirect(self.showChannels(author, name))
                    .flashing("error" -> "A channel with that name already exists.")
              }
              case Some(channel) =>
                // Channel color taken
                Redirect(self.showChannels(author, name))
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
    * @param name         Project name
    * @param channelName  Channel name
    * @return             View of channels
    */
  def editChannel(author: String, name: String, channelName: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      // Get all channels
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) =>
          val form = Forms.ChannelEdit.bindFromRequest.get
          val newName = form._1
          // Find submitted channel by old name
          channels.find(c => c.getName.equals(channelName)) match {
            case None => NotFound("Channel not found.")
            case Some(channel) => ChannelColors.values.find(color => color.hex.equalsIgnoreCase(form._2)) match {
              case None => BadRequest("Invalid channel color.")
              case Some(color) =>
                // Check if color is taken by different channel
                val colorChan = channels.find(c => c.getColor.equals(color))
                val colorTaken = colorChan.isDefined && !colorChan.get.equals(channel)

                // Check if name taken by different channel
                val nameChan = channels.find(c => c.getName.equals(newName))
                val nameTaken = nameChan.isDefined && !nameChan.get.equals(channel)

                if (colorTaken) {
                  Redirect(self.showChannels(author, name))
                    .flashing("error" -> "A channel with that color already exists.")
                } else if (nameTaken) {
                  Redirect(self.showChannels(author, name))
                    .flashing("error" -> "A channel with that name already exists.")
                } else {
                  // Change name if different
                  if (!channelName.equals(newName)) {
                    Storage.now(channel.setName(project, newName)) match {
                      case Failure(thrown) => throw thrown
                      case Success(i) => ;
                    }
                  }

                  // Change color if different
                  if (!channel.getColor.equals(color)) {
                    Storage.now(channel.setColor(color)) match {
                      case Failure(thrown) => throw thrown
                      case Success(i) => ;
                    }
                  }

                  Redirect(self.showChannels(author, name))
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
    * @param name         Project name
    * @param channelName  Channel name
    * @return             View of channels
    */
  def deleteChannel(author: String, name: String, channelName: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      Storage.now(project.getChannels) match {
        case Failure(thrown) => throw thrown
        case Success(channels) => if (channels.size == 1) {
          Redirect(self.showChannels(author, name))
            .flashing("error" -> "You cannot delete your only channel.")
        } else {
          channels.find(c => c.getName.equals(channelName)) match {
            case None => NotFound
            case Some(channel) => channel.delete(project) match {
              case Failure(thrown) => throw thrown
              case Success(void) => Redirect(self.showChannels(author, name))
            }
          }
        }
      }
    }))
  }

  /**
    * Displays the "discussion" tab within a Project view.
    *
    * @param author   Owner of project
    * @param name     Name of project
    * @return         View of project
    */
  def showDiscussion(author: String, name: String) = Action { implicit request =>
    withProject(author, name, project => Ok(views.projects.discussion(project)))
  }

  /**
    * Shows the project manager or "settings" pane.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Project manager
    */
  def showManager(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => Ok(views.projects.manage(project))))
  }

  /**
    * Renames the specified project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Project homepage
    */
  def rename(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      val newName = Forms.ProjectRename.bindFromRequest.get
      Storage.now(project.setName(newName)) match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(self.show(author, newName))
      }
    }))
  }

  /**
    * Irreversibly deletes the specified project.
    *
    * @param author   Project owner
    * @param name     Project name
    * @return         Home page
    */
  def delete(author: String, name: String) = { withUser(Some(author), user => implicit request =>
    withProject(author, name, project => {
      project.delete match {
        case Failure(thrown) => throw thrown
        case Success(i) => Redirect(routes.Application.index(None))
          .flashing("success" -> ("Project \"" + project.getName + "\" deleted."))
      }
    }))
  }

}
