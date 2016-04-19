package ore.project

import java.nio.file.Files

import com.google.common.base.Preconditions._
import db.query.Queries
import db.query.Queries.now
import models.project.Project.PendingProject
import models.project.Version.PendingVersion
import models.project.{Channel, Project, Version}
import models.user.{ProjectRole, User}
import ore.Colors.Color
import ore.permission.role.RoleTypes
import ore.project.ProjectFiles._
import play.api.libs.Files.TemporaryFile
import util.P._

import scala.util.Try

/**
  * Handles creation of Project's and their components.
  */
object ProjectFactory {

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def initUpload(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
    val tmpPath = TempDir.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)
    if (Files.notExists(tmpPath.getParent)) Files.createDirectories(tmpPath.getParent)
    tmp.moveTo(plugin.path.toFile, replace = true)
    plugin.loadMeta
    plugin
  }

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    * @throws         IllegalArgumentException if the project already exists
    */
  def createProject(pending: PendingProject): Try[Project] = Try {
    checkArgument(!pending.project.exists, "project already exists", "")
    checkArgument(pending.project.isNamespaceAvailable, "slug not available", "")
    checkArgument(Project.isValidName(pending.project.name), "invalid name", "")
    val newProject = now(Queries.Projects create pending.project).get

    // Add Project roles
    val user = pending.firstVersion.owner
    user.projectRoles.add(new ProjectRole(user.id.get, RoleTypes.ProjectOwner, newProject.id.get))
    for (role <- pending.roles) {
      User.withId(role.userId).get.projectRoles.add(role.copy(projectId=newProject.id.get))
    }

    newProject
  }

  /**
    * Creates a new version from the specified PendingVersion.
    *
    * @param pending  PendingVersion
    * @return         New version
    */
  def createVersion(pending: PendingVersion): Try[Version] = Try {
    var channel: Channel = null
    val project = Project.withSlug(pending.owner, pending.projectSlug).get

    // Create channel if not exists
    project.channels.withName(pending.channelName) match {
      case None => channel = project.addChannel(pending.channelName, pending.channelColor)
      case Some(existing) => channel = existing
    }

    // Create version
    val pendingVersion = pending.version
    if (channel.versions.withName(pendingVersion.versionString).isDefined) {
      throw new IllegalArgumentException("Version already exists.")
    }

    var newVersion = new Version(pendingVersion.versionString, pendingVersion.dependenciesIds,
                                 pendingVersion.description.orNull, pendingVersion.assets.orNull,
                                 project.id.get, channel.id.get, pending.plugin.path.toFile.length)

    newVersion = channel.versions.add(newVersion)
    uploadPlugin(channel, pending.plugin)
    newVersion
  }

  private def uploadPlugin(channel: Channel, plugin: PluginFile): Try[Unit] = Try {
    val meta = plugin.meta.get
    var oldPath = plugin.path
    if (!plugin.isZipped) oldPath = plugin.zip
    val newPath = uploadPath(plugin.owner.username, meta.getName, meta.getVersion, channel.name)
    if (!Files.exists(newPath.getParent)) Files.createDirectories(newPath.getParent)
    Files.move(oldPath, newPath)
    Files.delete(oldPath)
  }

}
