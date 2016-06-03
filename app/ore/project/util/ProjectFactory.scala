package ore.project.util

import java.nio.file.Files._
import javax.inject.Inject

import com.google.common.base.Preconditions._
import db.ModelService
import db.impl.service.{ProjectBase, UserBase}
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import models.user.{ProjectRole, User}
import ore.permission.role.RoleTypes
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.cache.CacheApi
import play.api.libs.Files.TemporaryFile
import util.OreConfig
import util.StringUtils._

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Handles creation of Project's and their components.
  */
trait ProjectFactory {

  implicit val service: ModelService
  implicit val config: OreConfig
  implicit val forums: DiscourseApi

  implicit val users: UserBase = this.service.access(classOf[UserBase])
  implicit val projects: ProjectBase = this.service.access(classOf[ProjectBase])

  import service.processor.process

  val manager: ProjectManager
  val fileManager: ProjectFileManager = this.manager.fileManager
  val cacheApi: CacheApi
  val env = this.fileManager.env

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def processPluginFile(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
    if (!name.endsWith(".zip") && !name.endsWith(".jar")) {
      throw new InvalidPluginFileException("Plugin file must be either a JAR or ZIP file.")
    }

    val tmpPath = this.env.tmp.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)

    if (notExists(tmpPath.getParent)) createDirectories(tmpPath.getParent)
    val oldPath = tmp.file.toPath
    tmp.moveTo(plugin.path.toFile, replace = true)

    if (this.config.projects.getBoolean("tmp-file-save").get) copy(plugin.path, oldPath)

    val meta = plugin.loadMeta
    val pathStr = plugin.path.toString
    val ext = pathStr.substring(pathStr.lastIndexOf('.'))
    plugin.move(plugin.path.getParent.resolve(meta.getName + '-' + meta.getVersion + ext))

    plugin
  }

  /**
    * Creates a new Project from the specified PluginMetadata.
    *
    * @param owner  Owner of project
    * @param meta   PluginMetadata object
    * @return       New project
    */
  def projectFromMeta(owner: User, meta: PluginMetadata): Project = process {
    new Project(meta.getId, meta.getName, owner.username, owner.id.get, meta.getUrl)
  }

  /**
    * Creates a new [[Project]] [[Version]] from the specified [[PluginMetadata]].
    *
    * @param project Project of version
    * @param plugin  Plugin file
    * @return
    */
  def versionFromFile(project: Project, plugin: PluginFile): Version = {
    val meta = plugin.meta.get
    val depends = for (depend <- meta.getRequiredDependencies.asScala) yield depend.getId + ":" + depend.getVersion
    val path = plugin.path
    process {
      new Version(
        versionString = meta.getVersion,
        dependencyIds = depends.toList,
        _description = Option(meta.getDescription),
        projectId = project.id.getOrElse(-1),
        fileSize = path.toFile.length,
        hash = plugin.md5,
        fileName = path.getFileName.toString
      )
    }
  }

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project        Project that is pending
    * @param firstVersion   Uploaded plugin
    */
  def setProjectPending(project: Project, firstVersion: PluginFile): PendingProject =  {
    val pendingProject = PendingProject(
      this.manager, this, project, firstVersion, this.config, Set.empty, this.cacheApi
    )
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
  = this.cacheApi.get[PendingProject](owner + '/' + slug)

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
    val pending = PendingVersion(
      manager = this.manager,
      factory = this,
      owner = owner,
      projectSlug = slug,
      channelName = channel,
      channelColor = this.config.defaultChannelColor,
      version = version,
      plugin = plugin,
      cacheApi = cacheApi
    )
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
  = this.cacheApi.get[PendingVersion](owner + '/' + slug + '/' + version)

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    * @throws         IllegalArgumentException if the project already exists
    */
  def createProject(pending: PendingProject): Try[Project] = Try {
    val project = pending.project
    checkArgument(!this.projects.exists(project), "project already exists", "")
    checkArgument(this.projects.isNamespaceAvailable(project.ownerName, project.slug), "slug not available", "")
    checkArgument(this.config.isValidProjectName(pending.project.name), "invalid name", "")
    val newProject = this.projects.add(pending.project)

    // Add Project roles
    val user = pending.file.user
    user.projectRoles.add(new ProjectRole(user.id.get, RoleTypes.ProjectOwner, newProject.id.get))
    for (role <- pending.roles) {
      this.users.get(role.userId).get.projectRoles.add(role.copy(projectId=newProject.id.get))
    }

    this.forums.embed.createTopic(newProject)
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
    val project = this.projects.withSlug(pending.owner, pending.projectSlug).get

    // Create channel if not exists
    project.channels.find(equalsIgnoreCase(_.name, pending.channelName)) match {
      case None => channel = project.addChannel(pending.channelName, pending.channelColor)
      case Some(existing) => channel = existing
    }

    // Create version
    val pendingVersion = pending.version
    if (pendingVersion.exists && this.config.projects.getBoolean("file-validate").get) {
      throw new IllegalArgumentException("Version already exists.")
    }

    val newVersion = channel.versions.add(new Version(
      versionString = pendingVersion.versionString,
      dependencyIds = pendingVersion.dependencyIds,
      _description = pendingVersion.description,
      assets = pendingVersion.assets,
      projectId = project.id.get,
      channelId = channel.id.get,
      fileSize = pendingVersion.fileSize,
      hash = pendingVersion.hash,
      fileName = pendingVersion.fileName
    ))

    uploadPlugin(channel, pending.plugin)
    newVersion
  }

  private def uploadPlugin(channel: Channel, plugin: PluginFile): Try[Unit] = Try {
    val meta = plugin.meta.get
    var oldPath = plugin.path
    val newPath = this.fileManager.getProjectDir(plugin.user.username, meta.getName).resolve(plugin.path.getFileName)
    if (!exists(newPath.getParent)) createDirectories(newPath.getParent)
    move(oldPath, newPath)
    delete(oldPath)
  }

}

class OreProjectFactory @Inject()(override val service: ModelService,
                                  override val config: OreConfig,
                                  override val forums: DiscourseApi,
                                  override val manager: ProjectManager,
                                  override val cacheApi: CacheApi)
                                  extends ProjectFactory
