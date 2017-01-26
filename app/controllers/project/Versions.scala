package controllers.project

import java.io.InputStream
import java.net.{InetAddress, URI}
import java.nio.file.Files._
import java.nio.file.{Files, StandardCopyOption}
import java.sql.Timestamp
import java.util.{Date, UUID}
import javax.inject.Inject

import com.github.tminglei.slickpg.InetString
import controllers.BaseController
import controllers.sugar.Requests.ProjectRequest
import db.ModelService
import db.impl.OrePostgresDriver.api._
import discourse.OreDiscourseApi
import form.OreForms
import models.project._
import ore.permission.{EditVersions, ReviewProjects}
import ore.project.Dependency
import ore.project.factory.{PendingProject, ProjectFactory}
import ore.project.io.DownloadTypes._
import ore.project.io.{DownloadTypes, InvalidPluginFileException, PluginFile, PluginUpload}
import ore.{OreConfig, OreEnv, StatTracker}
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import security.spauth.SingleSignOnConsumer
import util.StringUtils._
import views.html.projects.{versions => views}

import scala.util.Try

/**
  * Controller for handling Version related actions.
  */
class Versions @Inject()(stats: StatTracker,
                         forms: OreForms,
                         factory: ProjectFactory,
                         forums: OreDiscourseApi,
                         implicit override val sso: SingleSignOnConsumer,
                         implicit override val messagesApi: MessagesApi,
                         implicit override val env: OreEnv,
                         implicit override val config: OreConfig,
                         implicit override val service: ModelService)
                         extends BaseController {

  private val fileManager = this.projects.fileManager
  private val self = controllers.project.routes.Versions
  private val warnings = this.service.access[DownloadWarning](classOf[DownloadWarning])

  private def VersionEditAction(author: String, slug: String)
  = AuthedProjectAction(author, slug, requireUnlock = true) andThen ProjectPermissionAction(EditVersions)

  /**
    * Shows the specified version view page.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Version view
    */
  def show(author: String, slug: String, versionString: String) = ProjectAction(author, slug) { implicit request =>
    implicit val project = request.project
    withVersion(versionString) { version =>
      this.stats.projectViewed { implicit request =>
        Ok(views.view(project, version.channel, version))
      }
    }
  }

  /**
    * Saves the specified Version's description.
    *
    * @param author        Project owner
    * @param slug          Project slug
    * @param versionString Version name
    * @return View of Version
    */
  def saveDescription(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        version.description = this.forms.VersionDescription.bindFromRequest.get.trim
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Sets the specified Version as the recommended download.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version name
    * @return               View of version
    */
  def setRecommended(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        project.recommendedVersion = version
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Sets the specified Version as approved by the moderation staff.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version name
    * @return               View of version
    */
  def approve(author: String, slug: String, versionString: String) = {
    (AuthedProjectAction(author, slug, requireUnlock = true)
      andThen ProjectPermissionAction(ReviewProjects)) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        version.setReviewed(reviewed = true)
        Redirect(self.show(author, slug, versionString))
      }
    }
  }

  /**
    * Displays the "versions" tab within a Project view.
    *
    * @param author   Owner of project
    * @param slug     Project slug
    * @param channels Visible channels
    * @return View of project
    */
  def showList(author: String, slug: String, channels: Option[String], page: Option[Int]) = {
    ProjectAction(author, slug) { implicit request =>
      val project = request.project
      val allChannels = project.channels.toSeq

      var visibleNames: Option[Array[String]] = channels.map(_.toLowerCase.split(','))
      val visible: Option[Array[Channel]] = visibleNames.map(_.map { name =>
        allChannels.find(_.name.equalsIgnoreCase(name)).get
      })

      val visibleIds: Array[Int] = visible.map(_.map(_.id.get)).getOrElse(allChannels.map(_.id.get).toArray)

      val pageSize = this.config.projects.getInt("init-version-load").get
      val p = page.getOrElse(1)
      val versions = project.versions.sorted(
        ordering = _.createdAt.desc,
        filter = _.channelId inSetBind visibleIds,
        offset = pageSize * (p - 1),
        limit = pageSize)

      if (visibleNames.isDefined && visibleNames.get.toSet.equals(allChannels.map(_.name.toLowerCase).toSet)) {
        visibleNames = None
      }

      this.stats.projectViewed { implicit request =>
        Ok(views.list(project, allChannels, versions, visibleNames, p))
      }
    }
  }

  /**
    * Shows the creation form for new versions on projects.
    *
    * @param author Owner of project
    * @param slug   Project slug
    * @return Version creation view
    */
  def showCreator(author: String, slug: String) = VersionEditAction(author, slug) { implicit request =>
    val project = request.project
    Ok(views.create(project, None, Some(project.channels.all.toSeq), showFileControls = true))
  }

  /**
    * Uploads a new version for a project for further processing.
    *
    * @param author Owner name
    * @param slug   Project slug
    * @return Version create page (with meta)
    */
  def upload(author: String, slug: String) = VersionEditAction(author, slug) { implicit request =>
    val call = self.showCreator(author, slug)
    val user = request.user
    this.factory.getUploadError(user) match {
      case Some(error) =>
        Redirect(call).withError(error)
      case None =>
        PluginUpload.bindFromRequest() match {
          case None =>
            Redirect(call).withError("error.noFile")
          case Some(uploadData) =>
            try {
              val plugin = this.factory.processPluginUpload(uploadData, user)
              val project = request.project
              if (!plugin.meta.get.getId.equals(project.pluginId))
                Redirect(call).withError("error.version.invalidPluginId")
              else {
                val version = this.factory.startVersion(plugin, project, project.channels.all.head.name)
                val model = version.underlying
                if (model.exists && this.config.projects.getBoolean("file-validate").get)
                  Redirect(call).withError("error.version.duplicate")
                else if (project.isSpongePlugin && !model.hasDependency(Dependency.SpongeApiId))
                  Redirect(call).withError("error.version.noDependency.sponge")
                else if (project.isForgeMod && !model.hasDependency(Dependency.ForgeId))
                  Redirect(call).withError("error.version.noDependency.forge")
                else {
                  version.cache()
                  Redirect(self.showCreatorWithMeta(project.ownerName, slug, model.versionString))
                }
              }
            } catch {
              case e: InvalidPluginFileException =>
                Redirect(call).withError(e.getMessage)
            }
        }
    }
  }

  /**
    * Displays the "version create" page with the associated plugin meta-data.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Version create view
    */
  def showCreatorWithMeta(author: String, slug: String, versionString: String) = {
    UserLock(ShowProject(author, slug)) { implicit request =>
      // Get pending version
      this.factory.getPendingVersion(author, slug, versionString) match {
        case None =>
          Redirect(self.showCreator(author, slug))
        case Some(pendingVersion) =>
          // Get project
          pendingOrReal(author, slug) match {
            case None =>
              Redirect(self.showCreator(author, slug))
            case Some(p) => p match {
              case pending: PendingProject =>
                Ok(views.create(pending.underlying, Some(pendingVersion), None, showFileControls = false))
              case real: Project =>
                Ok(views.create(real, Some(pendingVersion), Some(real.channels.toSeq), showFileControls = true))
            }
          }
      }
    }
  }

  private def pendingOrReal(author: String, slug: String): Option[Any] = {
    // Returns either a PendingProject or existing Project
    this.projects.withSlug(author, slug) match {
      case None => this.factory.getPendingProject(author, slug)
      case Some(project) => Some(project)
    }
  }

  /**
    * Completes the creation of the specified pending version or project if
    * first version.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return New version view
    */
  def publish(author: String, slug: String, versionString: String) = {
    UserLock(ShowProject(author, slug)) { implicit request =>
      // First get the pending Version
      this.factory.getPendingVersion(author, slug, versionString) match {
        case None =>
          // Not found
          Redirect(self.showCreator(author, slug))
        case Some(pendingVersion) =>
          // Get submitted channel
          this.forms.VersionCreate.bindFromRequest.fold(
            hasErrors => {
              // Invalid channel
              val call = self.showCreatorWithMeta(author, slug, versionString)
              Redirect(call).withError(hasErrors.errors.head.message)
            },

            versionData => {
              // Channel is valid
              pendingVersion.channelName = versionData.channelName.trim
              pendingVersion.channelColor = versionData.color

              // Check for pending project
              this.factory.getPendingProject(author, slug) match {
                case None =>
                  // No pending project, create version for existing project
                  withProject(author, slug) { project =>
                    val existingChannel = project.channels.find {
                      equalsIgnoreCase(_.name, pendingVersion.channelName)
                    }.orNull

                    var channelResult: Either[String, Channel] = Right(existingChannel)
                    if (existingChannel == null)
                      channelResult = versionData.addTo(project)

                    channelResult.fold(
                      error => {
                        Redirect(self.showCreatorWithMeta(author, slug, versionString)).withError(error)
                      },
                      channel => {
                        val newVersion = pendingVersion.complete().get
                        if (versionData.recommended)
                          project.recommendedVersion = newVersion

                        // Create forum topic reply / update description
                        versionData.content.foreach { content =>
                          val c = content.trim
                          newVersion.description = c
                          if (project.topicId != -1)
                            this.forums.postVersionRelease(project, newVersion, c)
                        }

                        Redirect(self.show(author, slug, versionString))
                      }
                    )
                  }
                case Some(pendingProject) =>
                  // Found a pending project, create it with first version
                  pendingProject.complete().get
                  Redirect(ShowProject(author, slug))
              }
            }
          )
      }
    }
  }

  /**
    * Deletes the specified version and returns to the version page.
    *
    * @param author        Owner name
    * @param slug          Project slug
    * @param versionString Version name
    * @return Versions page
    */
  def delete(author: String, slug: String, versionString: String) = {
    VersionEditAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString) { version =>
        this.projects.deleteVersion(version)
        Redirect(self.showList(author, slug, None, None))
      }
    }
  }

  /**
    * Displays a confirmation view for downloading unreviewed versions. The
    * client is issued a unique token that will be checked once downloading to
    * ensure that they have landed on this confirmation before downloading the
    * version.
    *
    * @param author Project author
    * @param slug   Project slug
    * @param target Target version
    * @param origin Origin URL
    * @return       Confirmation view
    */
  def showDownloadConfirm(author: String,
                          slug: String,
                          target: String,
                          origin: Option[String],
                          downloadType: Option[Int]) = {
    ProjectAction(author, slug) { implicit request =>
      val dlType = downloadType.flatMap(i => DownloadTypes.values.find(_.id == i)).getOrElse(DownloadTypes.UploadedFile)
      implicit val project = request.project
      withVersion(target) { version =>
        if (version.isReviewed)
          Redirect(self.download(author, slug, target))
        else if (origin.isDefined && !isValidRedirect(origin.get))
          Redirect(ShowProject(author, slug))
        else {
          val userAgent = request.headers.get("User-Agent")
          var cliClient: Boolean = false
          if (userAgent.isDefined) {
            val ua = userAgent.get.toLowerCase
            if (ua.startsWith("wget/") || ua.startsWith("curl/"))
              cliClient = true
          }

          // generate a unique "warning" object to ensure the user has landed
          // on the warning before downloading
          val token = UUID.randomUUID().toString
          val expiration = new Timestamp(new Date().getTime + this.config.security.getLong("unsafeDownload.maxAge").get)
          val address = InetString(StatTracker.remoteAddress)
          // remove old warning attached to address
          this.warnings.removeAll(_.address === address)
          // create warning
          val warning = this.warnings.add(DownloadWarning(
            expiration = expiration,
            token = token,
            versionId = version.id.get,
            address = InetString(StatTracker.remoteAddress)))

          if (!cliClient) {
            Ok(views.unsafeDownload(project, version, origin, dlType)).withCookies(warning.cookie)
          } else {
            MultiStatus(this.messagesApi(
              "version.download.confirm.body.plain",
              self.downloadUnsafely(author, slug, target, origin, downloadType, Some(token)).absoluteURL()))
          }
        }
      }
    }
  }

  /**
    * Verifies that the client has landed on a confirmation page for the
    * target version and sends the file.
    *
    * @param author Project author
    * @param slug   Project slug
    * @param target Target version
    * @param origin Origin URL
    * @return       Unreviewed file
    */
  def downloadUnsafely(author: String,
                       slug: String,
                       target: String,
                       origin: Option[String],
                       downloadType: Option[Int],
                       token: Option[String]) = {
    ProjectAction(author, slug) { implicit request =>
      val dlType = downloadType.flatMap(i => DownloadTypes.values.find(_.id == i)).getOrElse(UploadedFile)
      implicit val project = request.project
      withVersion(target)(sendUnsafely(project, _, origin, dlType, token))
    }
  }

  private def sendUnsafely(project: Project,
                           version: Version,
                           origin: Option[String],
                           downloadType: DownloadType,
                           token: Option[String])(implicit request: ProjectRequest[_]): Result = {
    val author = project.ownerName
    val slug = project.slug
    val target = version.name
    if (version.isReviewed)
      Redirect(self.download(author, slug, target))
    else if (origin.isDefined && !isValidRedirect(origin.get))
      Redirect(ShowProject(author, slug))
    else if (token.isEmpty && request.cookies.get(DownloadWarning.COOKIE).isEmpty)
      Redirect(self.showDownloadConfirm(author, slug, target, origin, Some(downloadType.id)))
    else {
      val tokenValue = token.orElse(request.cookies.get(DownloadWarning.COOKIE).map(_.value)).get
      // find unexpired warning of token
      val warning = this.warnings.find(_.token === tokenValue).flatMap { warning =>
        if (warning.hasExpired) {
          warning.remove()
          None
        } else
          Some(warning)
      }

      // verify the user has landed on a confirmation for this version before
      warning match {
        case None =>
          Redirect(self.showDownloadConfirm(author, slug, target, origin, Some(downloadType.id)))
        case Some(warn) =>
          // make sure the client is downloading from the same address as
          // they confirmed from
          val address = InetString(StatTracker.remoteAddress)
          val addrMatch = InetAddress.getByName(warn.address.address)
            .equals(InetAddress.getByName(address.address))
          if (warn.versionId != version.id.get || !addrMatch) {
            warn.remove()
            Redirect(self.showDownloadConfirm(author, slug, target, origin, Some(downloadType.id)))
          } else {
            // create a record of this download
            val downloads = this.service.access[UnsafeDownload](classOf[UnsafeDownload])
            val userId = this.users.current.flatMap(_.id)
            val download = downloads.add(UnsafeDownload(
              userId = userId,
              address = address,
              downloadType = downloadType))
            warn.download = download
            downloadType match {
              case UploadedFile =>
                sendVersion(project, version, confirmed = true)
              case JarFile =>
                sendJar(project, version, confirmed = true)
              case SignatureFile =>
                // Note: Shouldn't get here in the first place since sig files
                // don't need confirmation, but added as a failsafe.
                sendSignatureFile(version)
              case _ =>
                throw new Exception("unknown download type: " + downloadType)
            }
          }
      }
    }
  }

  private def isValidRedirect(url: String)
  = Try(!new URI(url).isAbsolute).toOption.getOrElse(false) && url.startsWith("/") && !url.startsWith("//")

  /**
    * Sends the specified Project Version to the client.
    *
    * @param author        Project owner
    * @param slug          Project slug
    * @param versionString Version string
    * @return Sent file
    */
  def download(author: String, slug: String, versionString: String) = ProjectAction(author, slug) { implicit request =>
    implicit val project = request.project
    withVersion(versionString) { version =>
      sendVersion(project, version, confirmed = false)
    }
  }

  /**
    * Sends the specified project's current recommended version to the client.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return Sent file
    */
  def downloadRecommended(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    val project = request.project
    val rv = project.recommendedVersion
    sendVersion(project, rv, confirmed = false)
  }

  private def sendVersion(project: Project,
                          version: Version,
                          confirmed: Boolean)(implicit req: ProjectRequest[_]): Result = {
    if (!confirmed && !version.isReviewed)
      Redirect(self.showDownloadConfirm(project.ownerName, project.slug, version.name, None, Some(UploadedFile.id)))
    else {
      this.stats.versionDownloaded(version) { implicit request =>
        Ok.sendFile(this.fileManager.getProjectDir(project.ownerName, project.name).resolve(version.fileName).toFile)
      }
    }
  }

  /**
    * Downloads the specified version as a JAR regardless of the original
    * uploaded file type.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version name
    * @return               Sent file
    */
  def downloadJar(author: String, slug: String, versionString: String) = {
    ProjectAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString)(version => sendJar(project, version, confirmed = false))
    }
  }

  /**
    * Downloads the Project's recommended version as a JAR regardless of the
    * original uploaded file type.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Sent file
    */
  def downloadRecommendedJar(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    val project = request.project
    sendJar(project, project.recommendedVersion, confirmed = false)
  }

  /**
    * Downloads the specified version as a JAR regardless of the original
    * uploaded file type.
    *
    * @param pluginId       Project unique plugin ID
    * @param versionString  Version name
    * @return               Sent file
    */
  def downloadJarById(pluginId: String, versionString: String) = ProjectAction(pluginId) { implicit request =>
    implicit val project = request.project
    withVersion(versionString)(version => sendJar(project, version, confirmed = false))
  }

  /**
    * Downloads the Project's recommended version as a JAR regardless of the
    * original uploaded file type.
    *
    * @param pluginId Project unique plugin ID
    * @return         Sent file
    */
  def downloadRecommendedJarById(pluginId: String) = ProjectAction(pluginId) { implicit request =>
    val project = request.project
    sendJar(project, project.recommendedVersion, confirmed = false)
  }

  /**
    * Sends the specified Project Version signature file to the client.
    *
    * @param author         Project owner
    * @param slug           Project slug
    * @param versionString  Version string
    * @return               Sent file
    */
  def downloadSignature(author: String, slug: String, versionString: String) = {
    ProjectAction(author, slug) { implicit request =>
      implicit val project = request.project
      withVersion(versionString)(sendSignatureFile)
    }
  }

  /**
    * Downloads the signature file for the specified version.
    *
    * @param pluginId       Project unique plugin ID
    * @param versionString  Version name
    * @return               Sent file
    */
  def downloadSignatureById(pluginId: String, versionString: String) = ProjectAction(pluginId) { implicit request =>
    implicit val project = request.project
    withVersion(versionString)(sendSignatureFile)
  }

  /**
    * Downloads the signature file for the Project's recommended version.
    *
    * @param author Project owner
    * @param slug   Project slug
    * @return       Sent file
    */
  def downloadRecommendedSignature(author: String, slug: String) = ProjectAction(author, slug) { implicit request =>
    sendSignatureFile(request.project.recommendedVersion)
  }

  /**
    * Downloads the signature file for the Project's recommended version.
    *
    * @param pluginId Project unique plugin ID
    * @return         Sent file
    */
  def downloadRecommendedSignatureById(pluginId: String) = ProjectAction(pluginId) { implicit request =>
    sendSignatureFile(request.project.recommendedVersion)
  }

  private def sendJar(project: Project,
                      version: Version,
                      confirmed: Boolean)(implicit request: ProjectRequest[_]): Result = {
    if (!confirmed && !version.isReviewed)
      Redirect(self.showDownloadConfirm(project.ownerName, project.slug, version.name, None, Some(JarFile.id)))
    else {
      val fileName = version.fileName
      val path = this.fileManager.getProjectDir(project.ownerName, project.name).resolve(fileName)
      this.stats.versionDownloaded(version) { implicit request =>
        if (fileName.endsWith(".jar"))
          Ok.sendFile(path.toFile)
        else {
          val pluginFile = new PluginFile(path, signaturePath = null, project.owner.user)
          val jarName = fileName.substring(0, fileName.lastIndexOf('.')) + ".jar"
          val jarPath = this.fileManager.env.tmp.resolve(project.ownerName).resolve(jarName)

          var jarIn: InputStream = null
          try {
            jarIn = pluginFile.newJarStream
            copy(jarIn, jarPath, StandardCopyOption.REPLACE_EXISTING)
          } catch {
            case e: Exception =>
              Logger.error("an error occurred while trying to send a plugin", e)
          } finally {
            if (jarIn != null)
              jarIn.close()
            else
              Logger.error("could not obtain input stream for download request")
          }
          Ok.sendFile(jarPath.toFile, onClose = () => Files.delete(jarPath))
        }
      }
    }
  }

  private def sendSignatureFile(version: Version): Result = {
    val project = version.project
    val path = this.fileManager.getProjectDir(project.ownerName, project.name).resolve(version.signatureFileName)
    if (notExists(path)) {
      Logger.warn("project version missing signature file")
      NotFound
    } else
      Ok.sendFile(path.toFile)
  }

}
