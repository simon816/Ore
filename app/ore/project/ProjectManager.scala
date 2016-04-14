package ore.project

import java.nio.file.{Files, Path}

import com.google.common.base.Preconditions._
import db.query.Queries
import db.query.Queries.now
import models.project.Project.PendingProject
import models.project.Version.PendingVersion
import models.project.{Channel, Project, Version}
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes
import play.api.libs.Files.TemporaryFile
import util.Dirs._

import scala.util.Try

/**
  * Handles management of uploaded projects.
  */
object ProjectManager {

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def initUpload(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
    val tmpPath = Tmp.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)
    if (Files.notExists(tmpPath.getParent)) {
      Files.createDirectories(tmpPath.getParent)
    }
    tmp.moveTo(plugin.path.toFile, replace = true)
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
    val meta = plugin.meta.get
    var oldPath = plugin.path
    if (!plugin.isZipped) {
      oldPath = plugin.zip
    }
    val newPath = uploadPath(plugin.owner.username, meta.getName, meta.getVersion, channel.name)
    if (!Files.exists(newPath.getParent)) {
      Files.createDirectories(newPath.getParent)
    }
    Files.move(oldPath, newPath)
    Files.delete(oldPath)
  }

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    * @throws         IllegalArgumentException if the project already exists
    */
  def createProject(pending: PendingProject): Try[Project] = Try[Project] {
    checkArgument(!pending.project.exists, "project already exists", "")
    checkArgument(pending.project.isNamespaceAvailable, "slug not available", "")
    checkArgument(Project.isValidName(pending.project.name), "invalid name", "")
    val newProject = now(Queries.Projects.create(pending.project)).get
    val user = pending.firstVersion.owner
    user.projectRoles.add(new ProjectRole(user.id.get, RoleTypes.ProjectOwner, newProject.id.get))
    newProject
  }

  /**
    * Creates a new version from the specified PendingVersion.
    *
    * @param pending  PendingVersion
    * @return         New version
    */
  def createVersion(pending: PendingVersion): Try[Version] = Try[Version] {
    var channel: Channel = null
    val project = Project.withSlug(pending.owner, pending.projectSlug).get

    // Create channel if not exists
    project.channels.withName(pending.channelName) match {
      case None => channel = project.newChannel(pending.channelName, pending.channelColor).get
      case Some(existing) => channel = existing
    }

    // Create version
    val pendingVersion = pending.version
    if (channel.versions.withName(pendingVersion.versionString).isDefined) {
      throw new IllegalArgumentException("Version already exists.")
    }

    val newVersion = channel.newVersion(pendingVersion.versionString, pendingVersion.dependenciesIds,
      pendingVersion.description.orNull, pendingVersion.assets.orNull)
    uploadPlugin(channel, pending.plugin)
    newVersion
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
  def uploadPath(owner: String, name: String, version: String, channel: String): Path = {
    projectDir(owner, name).resolve(channel).resolve("%s-%s.zip".format(name, version.toLowerCase))
  }

  /**
    * Returns the specified project's plugin directory.
    *
    * @param owner  Owner name
    * @param name   Project name
    * @return       Plugin directory
    */
  def projectDir(owner: String, name: String): Path = {
    userDir(owner).resolve(name)
  }

  /**
    * Returns the specified user's plugin directory.
    *
    * @param owner  Owner name
    * @return       Plugin directory
    */
  def userDir(owner: String): Path = {
    Plugins.resolve(owner)
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
    val newProjectDir = projectDir(owner, newName)
    Files.move(projectDir(owner, oldName), newProjectDir)
    // Rename plugin files
    for (channelDir <- newProjectDir.toFile.listFiles()) {
      if (channelDir.isDirectory) {
        val channelName = channelDir.getName
        for (pluginFile <- channelDir.listFiles()) {
          val fileName = pluginFile.getName
          val versionString = fileName.substring(fileName.indexOf('-') + 1, fileName.lastIndexOf('.'))
          Files.move(pluginFile.toPath, uploadPath(owner, newName, versionString, channelName))
        }
      }
    }
  }

  /**
    * Renames the specified channel in the file system.
    *
    * @param owner        Project owner
    * @param projectName  Project name
    * @param oldName      Old channel name
    * @param newName      New channel name
    * @return             New path
    */
  def renameChannel(owner: String, projectName: String, oldName: String, newName: String): Try[Unit] = Try {
    val newPath = projectDir(owner, projectName).resolve(newName)
    val oldPath = projectDir(owner, projectName).resolve(oldName)
    Files.move(oldPath, newPath)
  }

}
