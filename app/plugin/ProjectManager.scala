package plugin

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
  * Handles management of uploaded projects.
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
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
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
    * @param plugin   PluginFile to upload
    * @return         Result
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

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    */
  def createProject(pending: PendingProject): Try[Project] = Try[Project] {
    if (pending.project.exists) {
      throw new IllegalArgumentException("Project already exists.")
    }

    Storage.now(Storage.createProject(pending.project)) match {
      case Failure(thrown) =>
        pending.cancel()
        throw thrown
      case Success(newProject) =>
        val docsDir = getDocsDir(newProject.owner, newProject.name)
        if (Files.notExists(docsDir)) {
          Files.createDirectories(docsDir)
        }
        Files.copy(CONF_DIR.resolve("markdown/Home.md"), docsDir.resolve("Home.md"))
        newProject
    }
  }

  /**
    * Creates a new version from the specified PendingVersion.
    *
    * @param pending  PendingVersion
    * @return         New version
    */
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

  /**
    * Updates the specified documentation page to the specified new name and
    * new content.
    *
    * @param owner        Project owner
    * @param projectName  Project name
    * @param oldName      Old page name
    * @param newName      New page name
    * @param content      Page content
    * @return             New page Path
    */
  def updatePage(owner: String, projectName: String, oldName: String, newName: String, content: String) = {
    val docsDir = getDocsDir(owner, projectName)
    Files.deleteIfExists(docsDir.resolve(oldName + ".md"))
    val path = docsDir.resolve(newName + ".md")
    Files.createFile(path)
    Files.write(path, content.getBytes("UTF-8"))
  }

  def pageExists(owner: String, projectName: String, page: String): Boolean = {
    println("pageExists(): " + page)
    Files.exists(getDocsDir(owner, projectName).resolve(page + ".md"))
  }

  def deletePage(owner: String, projectName: String, page: String) = {
    Files.deleteIfExists(getDocsDir(owner, projectName).resolve(page + ".md"))
  }

  def getPageNames(owner: String, projectName: String): Array[String] = {
    for (file <- getDocsDir(owner, projectName).toFile.listFiles)
      yield file.getName.substring(0, file.getName.lastIndexOf('.'))
  }

  /**
    * Returns the specified page's contents.
    *
    * @param owner        Owner name
    * @param projectName  Project name
    * @param page         Page name
    * @return             Page contents
    */
  def getPageContents(owner: String, projectName: String, page: String): Option[String] = {
    val path = getDocsDir(owner, projectName).resolve(page + ".md")
    if (Files.exists(path)) {
      Some(new String(Files.readAllBytes(path)))
    } else {
      None
    }
  }

  def fillPageTemplate(title: String): String = {
    new String(Files.readAllBytes(CONF_DIR.resolve("markdown/default.md"))).format(title)
  }

  /**
    * Returns the Path to where the specified Version should be.
    *
    * @param owner    Project owner
    * @param name     Project name
    * @param version  Project version
    * @param channel  Project channel
    * @return         Path to supposed file
    */
  def getUploadPath(owner: String, name: String, version: String, channel: String): Path = {
    getProjectDir(owner, name).resolve(channel).resolve("%s-%s.jar".format(name, version.toLowerCase))
  }

  /**
    * Returns the specified project's plugin directory.
    *
    * @param owner  Owner name
    * @param name   Project name
    * @return       Plugin directory
    */
  def getProjectDir(owner: String, name: String): Path = {
    getUserDir(owner).resolve(name)
  }

  /**
    * Returns the specified user's plugin directory.
    *
    * @param owner  Owner name
    * @return       Plugin directory
    */
  def getUserDir(owner: String): Path = {
    PLUGIN_DIR.resolve(owner)
  }

  /**
    * Returns the specified project's documentation directory.
    *
    * @param owner        Owner name
    * @param projectName  Project name
    * @return             Documentation directory
    */
  def getDocsDir(owner: String, projectName: String): Path = {
    DOCS_DIR.resolve(owner).resolve(projectName)
  }

  /**
    * Renames this specified project in the file system.
    *
    * @param owner    Owner name
    * @param oldName  Old project name
    * @param newName  New project name
    * @return         New path
    */
  def renameProject(owner: String, oldName: String, newName: String): Try[Unit] = Try {
    val newPath = getProjectDir(owner, newName)
    val oldPath = getProjectDir(owner, oldName)
    Files.move(oldPath, newPath)
    // TODO: Rename plugin files
  }

}
