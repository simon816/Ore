package ore.project.factory

import java.nio.file.Files._
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.common.base.Preconditions._
import db.ModelService
import db.impl.access.{ProjectBase, UserBase}
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import models.user.role.ProjectRole
import models.user.{Notification, User}
import ore.OreConfig
import ore.notification.NotificationTypes
import ore.permission.role.RoleTypes
import ore.project.NotifyWatchersTask
import ore.project.io.{InvalidPluginFileException, PluginFile, ProjectFileManager}
import org.spongepowered.plugin.meta.PluginMetadata
import play.api.cache.CacheApi
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import util.StringUtils._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

/**
  * Handles creation of Project's and their components.
  */
trait ProjectFactory {

  implicit val service: ModelService
  implicit val users: UserBase = this.service.access(classOf[UserBase])
  implicit val projects: ProjectBase = this.service.access(classOf[ProjectBase])

  val fileManager: ProjectFileManager = this.projects.fileManager
  val cacheApi: CacheApi
  val messages: MessagesApi
  val actorSystem: ActorSystem

  implicit val config: OreConfig
  implicit val forums: DiscourseApi
  implicit val env = this.fileManager.env

  import service.processor.process

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def processPluginFile(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
    if (!name.endsWith(".zip") && !name.endsWith(".jar"))
      throw InvalidPluginFileException("Plugin file must be either a JAR or ZIP file.")

    val tmpPath = this.env.tmp.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)

    if (notExists(tmpPath.getParent))
      createDirectories(tmpPath.getParent)
    val oldPath = tmp.file.toPath
    tmp.moveTo(plugin.path.toFile, replace = true)

    if (this.config.projects.getBoolean("tmp-file-save").get)
      copy(plugin.path, oldPath)

    val meta = plugin.loadMeta()
    val pathStr = plugin.path.toString
    val ext = pathStr.substring(pathStr.lastIndexOf('.'))
    val path = plugin.path.getParent.resolve(meta.getName + '-' + meta.getVersion + ext)
    if (!plugin.path.equals(path)) {
      deleteIfExists(path)
      plugin.move(path)
    }

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
    val depends = for (depend <- meta.getRequiredDependencies.asScala) yield
      depend.getId + ":" + depend.getVersion
    val path = plugin.path
    process {
      Version(
        versionString = meta.getVersion,
        mcversion = Option(meta.getMinecraftVersion),
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
      this.projects, this, project, firstVersion, this.config, Set.empty, this.cacheApi
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
    * @param project  Project version belongs to
    * @param channel  Name of channel
    * @param version  Name of version
    * @param plugin   Uploaded plugin
    */
  def setVersionPending(project: Project, channel: String, version: Version, plugin: PluginFile): PendingVersion = {
    val pending = PendingVersion(
      projects = this.projects,
      factory = this,
      project = project,
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

    // Invite members
    val dossier = newProject.memberships
    val owner = pending.file.user
    dossier.addRole(new ProjectRole(owner.id.get, RoleTypes.ProjectOwner, newProject.id.get, accepted = true))

    for (role <- pending.roles) {
      val user = role.user
      dossier.addRole(role.copy(projectId = newProject.id.get))
      user.sendNotification(Notification(
        originId = owner.id.get,
        notificationType = NotificationTypes.ProjectInvite,
        message = messages("notification.project.invite", role.roleType.title, project.name)
      ))
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
    val project = pending.project

    // Create channel if not exists
    project.channels.find(equalsIgnoreCase(_.name, pending.channelName)) match {
      case None => channel = project.addChannel(pending.channelName, pending.channelColor)
      case Some(existing) => channel = existing
    }

    // Create version
    val pendingVersion = pending.version
    if (pendingVersion.exists && this.config.projects.getBoolean("file-validate").get)
      throw new IllegalArgumentException("Version already exists.")

    val newVersion = channel.versions.add(Version(
      versionString = pendingVersion.versionString,
      mcversion = pendingVersion.mcversion,
      dependencyIds = pendingVersion.dependencyIds,
      _description = pendingVersion.description,
      assets = pendingVersion.assets,
      projectId = project.id.get,
      channelId = channel.id.get,
      fileSize = pendingVersion.fileSize,
      hash = pendingVersion.hash,
      fileName = pendingVersion.fileName
    ))

    // Notify watchers
    this.actorSystem.scheduler.scheduleOnce(Duration.Zero, NotifyWatchersTask(newVersion, messages))

    project.lastUpdated = this.service.theTime

    uploadPlugin(channel, pending.plugin)
    newVersion
  }

  private def uploadPlugin(channel: Channel, plugin: PluginFile): Try[Unit] = Try {
    val meta = plugin.meta.get
    val oldPath = plugin.path
    val newPath = this.fileManager.getProjectDir(plugin.user.username, meta.getName).resolve(plugin.path.getFileName)
    if (!exists(newPath.getParent))
      createDirectories(newPath.getParent)
    move(oldPath, newPath)
    delete(oldPath)
  }

}

class OreProjectFactory @Inject()(override val service: ModelService,
                                  override val config: OreConfig,
                                  override val forums: DiscourseApi,
                                  override val cacheApi: CacheApi,
                                  override val messages: MessagesApi,
                                  override val actorSystem: ActorSystem)
                                  extends ProjectFactory
