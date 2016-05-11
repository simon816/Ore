package ore

import java.nio.file.Files
import javax.inject.Inject

import com.google.common.base.Preconditions._
import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.{ProjectBase, UserBase}
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes
import ore.project.util.{PendingProject, PendingVersion, PluginFile, ProjectFileManager}
import org.apache.commons.io.FileUtils
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.cache.CacheApi
import play.api.libs.Files.TemporaryFile
import util.OreConfig
import util.StringUtils.equalsIgnoreCase

import scala.util.Try

/**
  * Handles creation of Project's and their components.
  */
trait ProjectFactory {

  implicit val projects: ProjectBase
  implicit val config: OreConfig
  implicit val users: UserBase
  implicit val forums: DiscourseApi

  val fileManager: ProjectFileManager
  val cacheApi: CacheApi
  val env = fileManager.env

  /**
    * Creates a new Project from the specified PluginMetadata.
    *
    * @param owner  Owner of project
    * @param meta   PluginMetadata object
    * @return       New project
    */
  def fromMeta(owner: User, meta: PluginMetadata): Project = {
    val project = new Project(meta.getId, meta.getName, owner.username, owner.id.get, meta.getUrl)
    // Initialize with dependencies.
    // (other models have their dependencies added during processing)
    project.projectBase = this.projects
    project.config = this.config
    project.userBase = this.users
    project.forums = this.forums
    project
  }

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def cacheUpload(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
    val tmpPath = env.tmp.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)
    if (Files.notExists(tmpPath.getParent)) Files.createDirectories(tmpPath.getParent)
    val oldPath = tmp.file.toPath
    tmp.moveTo(plugin.path.toFile, replace = true)
    if (config.projects.getBoolean("tmp-file-save").get) Files.copy(plugin.path, oldPath)
    plugin.loadMeta
    plugin
  }

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project        Project that is pending
    * @param firstVersion   Uploaded plugin
    */
  def setProjectPending(project: Project, firstVersion: PluginFile): PendingProject =  {
    val pendingProject = PendingProject(this, project, firstVersion, this.config, Set(), cacheApi)
    pendingProject.cache()
    pendingProject
  }

  /**
    * Returns the PendingProject of the specified owner and name, if any.
    *
    * @param owner  Project owner
    * @param slug   Project slug
    * @return       PendingProject if present, None otherwise
    */
  def getPendingProject(owner: String, slug: String): Option[PendingProject]
  = cacheApi.get[PendingProject](owner + '/' + slug)

  /**
    * Marks the specified Version as pending and caches it for later use.
    *
    * @param owner    Name of owner
    * @param slug     Project slug
    * @param channel  Name of channel
    * @param version  Name of version
    * @param plugin   Uploaded plugin
    */
  def setVersionPending(owner: String, slug: String, channel: String,
                        version: Version, plugin: PluginFile): PendingVersion = {
    val pending = PendingVersion(this, config, owner, slug, channel, Channel.DefaultColor, version, plugin, cacheApi)
    pending.cache()
    pending
  }

  /**
    * Returns the pending version for the specified owner, name, channel, and
    * version string.
    *
    * @param owner    Name of owner
    * @param slug     Project slug
    * @param version  Name of version
    * @return         PendingVersion, if present, None otherwise
    */
  def getPendingVersion(owner: String, slug: String, version: String): Option[PendingVersion]
  = cacheApi.get[PendingVersion](owner + '/' + slug + '/' + version)

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    * @throws         IllegalArgumentException if the project already exists
    */
  def createProject(pending: PendingProject): Try[Project] = Try {
    val project = pending.project
    checkArgument(!projects.exists(project), "project already exists", "")
    checkArgument(projects.isNamespaceAvailable(project.ownerName, project.slug), "slug not available", "")
    checkArgument(Project.isValidName(pending.project.name), "invalid name", "")
    val newProject = projects.access.add(pending.project)

    // Add Project roles
    val user = pending.file.user
    user.projectRoles.add(new ProjectRole(user.id.get, RoleTypes.ProjectOwner, newProject.id.get))
    for (role <- pending.roles) {
      users.access.get(role.userId).get.projectRoles.add(role.copy(projectId=newProject.id.get))
    }

    forums.embed.createTopic(newProject)
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
    val project = projects.withSlug(pending.owner, pending.projectSlug).get

    // Create channel if not exists
    project.channels.find(equalsIgnoreCase(_.name, pending.channelName)) match {
      case None => channel = project.addChannel(pending.channelName, pending.channelColor)
      case Some(existing) => channel = existing
    }

    // Create version
    val pendingVersion = pending.version
    if (pendingVersion.exists && config.projects.getBoolean("file-validate").get) {
      throw new IllegalArgumentException("Version already exists.")
    }

    var newVersion = new Version(
      pendingVersion.versionString, pendingVersion.dependenciesIds, pendingVersion.description.orNull,
      pendingVersion.assets.orNull, project.id.get, channel.id.get, pendingVersion.fileSize, pendingVersion.hash
    )

    newVersion = channel.versions.add(newVersion)
    uploadPlugin(channel, pending.plugin)
    newVersion
  }

  /**
    * Irreversibly deletes this project.
    *
    * @param project Project to delete
    */
  def deleteProject(project: Project) = {
    FileUtils.deleteDirectory(fileManager.projectDir(project.ownerName, project.name).toFile)
    if (project.topicId.isDefined) forums.embed.deleteTopic(project)
    project.remove()
  }

  /**
    * Irreversibly deletes this channel and all version associated with it.
    *
    * @param context Project context
    */
  def deleteChannel(channel: Channel)(implicit context: Project = null) = {
    val proj = if (context != null) context else channel.project
    checkArgument(proj.id.get == channel.projectId, "invalid proj id", "")

    val channels = proj.channels.all
    checkArgument(channels.size > 1, "only one channel", "")
    checkArgument(channel.versions.isEmpty || channels.count(c => c.versions.nonEmpty) > 1, "last non-empty channel", "")

    FileUtils.deleteDirectory(fileManager.projectDir(proj.ownerName, proj.name).resolve(channel.name).toFile)
    channel.remove()
  }

  /**
    * Irreversibly deletes this version.
    *
    * @param project Project context
    */
  def deleteVersion(version: Version)(implicit project: Project = null) = {
    val proj = if (project != null) project else version.project
    checkArgument(proj.versions.size > 1, "only one version", "")
    checkArgument(proj.id.get == version.projectId, "invalid context id", "")

    // Set recommended version to latest version if the deleted version was the rv
    val rv = proj.recommendedVersion
    if (this.equals(rv)) proj.recommendedVersion = proj.versions.sorted(_.createdAt.desc, limit = 1).head

    // Delete channel if now empty
    val channel: Channel = version.channel
    if (channel.versions.isEmpty) this.deleteChannel(channel)

    Files.delete(fileManager.uploadPath(proj.ownerName, proj.name, version.versionString))
    version.remove()
  }

  private def uploadPlugin(channel: Channel, plugin: PluginFile): Try[Unit] = Try {
    val meta = plugin.meta.get
    var oldPath = plugin.path
    if (!plugin.isZipped) oldPath = plugin.zip
    val newPath = fileManager.uploadPath(plugin.user.username, meta.getName, meta.getVersion)
    if (!Files.exists(newPath.getParent)) Files.createDirectories(newPath.getParent)
    Files.move(oldPath, newPath)
    Files.delete(oldPath)
  }

}

class OreProjectFactory @Inject()(service: ModelService,
                                  override val fileManager: ProjectFileManager,
                                  override val config: OreConfig,
                                  override val forums: DiscourseApi,
                                  override val cacheApi: CacheApi)
                                  extends ProjectFactory {
  override val users = service.getModelBase(classOf[UserBase])
  override val projects = service.getModelBase(classOf[ProjectBase])
}
