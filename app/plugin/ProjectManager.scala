package plugin

import java.nio.charset.Charset
import java.nio.file.{Files, Path}

import db.Storage
import models.auth.User
import models.project.Project.PendingProject
import models.project.Version.PendingVersion
import models.project.{Channel, Project, Version}
import play.api.Play
import play.api.Play.current
import play.api.libs.Files.TemporaryFile

import scala.util.{Failure, Success, Try}

/**
  * Handles file management of uploaded projects.
  */
object ProjectManager {

  val ROOT_DIR = Play.application.path.toPath
  val CONF_DIR = ROOT_DIR.resolve("conf")
  val UPLOADS_DIR = ROOT_DIR.resolve("uploads")
  val DOCS_DIR = UPLOADS_DIR.resolve("docs")
  val PLUGIN_DIR = UPLOADS_DIR.resolve("plugins")
  val TEMP_DIR = UPLOADS_DIR.resolve("tmp")

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp Temporary file
    * @param owner Project owner
    * @return New plugin file
    */
  def initUpload(tmp: TemporaryFile, owner: User): Try[PluginFile] = Try {
    val tmpPath = TEMP_DIR.resolve(owner.name).resolve("plugin.jar")
    val plugin = new PluginFile(tmpPath, owner)
    if (Files.notExists(tmpPath.getParent)) {
      Files.createDirectories(tmpPath.getParent)
    }
    tmp.moveTo(plugin.getPath.toFile, replace = true)
    plugin.loadMeta
    plugin
  }

  /**
    * Uploads the specified PluginFile to it's appropriate location.
    *
    * @param plugin PluginFile to upload
    * @return Result
    */
  def uploadPlugin(channel: Channel, plugin: PluginFile): Try[Unit] = Try {
    plugin.getMeta match {
      case None => throw new IllegalArgumentException("Specified PluginFile has no meta loaded.")
      case Some(meta) =>
        val oldPath = plugin.getPath
        val newPath = getUploadPath(plugin.getOwner.username, meta.getName, meta.getVersion, channel.name)
        if (!Files.exists(newPath.getParent)) {
          Files.createDirectories(newPath.getParent)
        }
        Files.move(oldPath, newPath)
        Files.delete(oldPath.getParent)
    }
  }

  def createProject(pending: PendingProject): Try[Project] = Try[Project] {
    Storage.now(Storage.createProject(pending.project)) match {
      case Failure(thrown) =>
        pending.cancel()
        throw thrown
      case Success(newProject) =>
        val docsDir = getDocsDir(newProject.owner, newProject.name)
        if (Files.notExists(docsDir)) {
          Files.createDirectories(docsDir)
        }
        Files.copy(CONF_DIR.resolve("default.md"), docsDir.resolve("home.md"))
        newProject
    }
  }

  def createVersion(pending: PendingVersion): Try[Version] = Try[Version] {
    // Get project
    Storage.now(Storage.getProject(pending.owner, pending.projectName)) match {
      case Failure(thrown) =>
        pending.cancel()
        throw thrown
      case Success(project) =>
        var channel: Channel = null
        // Create channel if not exists
        Storage.now(project.getChannel(pending.channelName)) match {
          case Failure(thrown) =>
            pending.cancel()
            throw thrown
          case Success(channelOpt) => channelOpt match {
            case None =>
              Storage.now(project.newChannel(pending.channelName)) match {
                case Failure(thrown) =>
                  pending.cancel()
                  throw thrown
                case Success(newChannel) => channel = newChannel
              }
            case Some(existingChannel) => channel = existingChannel
          }
        }

        // Create version
        val version = pending.version
        Storage.now(Storage.isDefined(Storage.getVersion(channel.id.get, version.versionString))) match {
          case Failure(ignored) => ;
          case Success(m) => throw new Exception("Version already exists.")
        }

        val versionResult = Storage.now(channel.newVersion(version.versionString, version.dependencies,
          version.description.orNull, version.assets.orNull))
        versionResult match {
          case Failure(thrown) =>
            pending.cancel()
            throw thrown
          case Success(newVersion) =>
            // Upload plugin file
            uploadPlugin(channel, pending.plugin)
            newVersion
        }
    }
  }

  def updatePage(owner: String, projectName: String, oldName: String, newName: String, content: String) = {
    val docsDir = getDocsDir(owner, projectName)
    Files.deleteIfExists(docsDir.resolve(oldName + ".md"))
    val path = docsDir.resolve(newName + ".md")
    Files.createFile(path)
    Files.write(path, content.getBytes("UTF-8"))
  }

  def getPageContents(owner: String, projectName: String, page: String): Option[String] = {
    val path = getDocsDir(owner, projectName).resolve(page + ".md")
    if (Files.exists(path)) {
      Some(new String(Files.readAllBytes(path)))
    } else {
      None
    }
  }

  /**
    * Returns the Path to where the specified Version should be.
    *
    * @param owner Project owner
    * @param name Project name
    * @param version Project version
    * @param channel Project channel
    * @return Path to supposed file
    */
  def getUploadPath(owner: String, name: String, version: String, channel: String): Path = {
    getProjectDir(owner, name).resolve(channel).resolve("%s-%s.jar".format(name, version.toLowerCase))
  }

  def getProjectDir(owner: String, name: String): Path = {
    getUserDir(owner).resolve(name)
  }

  def getUserDir(owner: String): Path = {
    PLUGIN_DIR.resolve(owner)
  }

  def getDocsDir(owner: String, projectName: String): Path = {
    DOCS_DIR.resolve(owner).resolve(projectName)
  }

  def renameProject(owner: String, oldName: String, newName: String): Try[Unit] = Try {
    val newPath = getProjectDir(owner, newName)
    Files.move(getProjectDir(owner, oldName), newPath)
    // TODO: Rename plugin files
  }

}
