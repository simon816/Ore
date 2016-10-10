package ore.project.factory

import java.nio.file.Files._
import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.common.base.Preconditions._
import db.ModelService
import db.impl.access.{ProjectBase, UserBase}
import discourse.impl.OreDiscourseApi
import models.project.{Channel, Project, Version}
import models.user.role.ProjectRole
import models.user.{Notification, User}
import ore.Colors.Color
import ore.OreConfig
import ore.permission.role.RoleTypes
import ore.project.NotifyWatchersTask
import ore.project.io.{InvalidPluginFileException, PluginFile, ProjectFileManager}
import ore.user.notification.NotificationTypes
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
  implicit val users: UserBase = this.service.getModelBase(classOf[UserBase])
  implicit val projects: ProjectBase = this.service.getModelBase(classOf[ProjectBase])

  val fileManager: ProjectFileManager = this.projects.fileManager
  val cacheApi: CacheApi
  val messages: MessagesApi
  val actorSystem: ActorSystem

  implicit val config: OreConfig
  implicit val forums: OreDiscourseApi
  implicit val env = this.fileManager.env

  /**
    * Loads a new [[PluginFile]] for further processing.
    *
    * @param uploadedFile File to process
    * @param name         File name
    * @param owner        User who uploaded the file
    * @return             Processed PluginFile
    */
  def processPluginFile(uploadedFile: TemporaryFile, name: String, owner: User): PluginFile = {
    if (!name.endsWith(".zip") && !name.endsWith(".jar"))
      throw InvalidPluginFileException("Plugin file must be either a JAR or ZIP file.")

    // Move file to temporary path
    val uploadPath = uploadedFile.file.toPath
    val tmpPath = this.env.tmp.resolve(owner.username).resolve(name)
    val plugin = new PluginFile(tmpPath, owner)
    if (notExists(tmpPath.getParent))
      createDirectories(tmpPath.getParent)
    uploadedFile.moveTo(plugin.path.toFile, replace = true)

    // Name the file correctly
    val metaData = plugin.loadMeta()
    val pathStr = plugin.path.toString
    val stopIndex = pathStr.lastIndexOf('.')
    if (stopIndex == -1)
      throw new IllegalStateException("file has no extension?")
    val extension = pathStr.substring(stopIndex)
    val namedPath = plugin.path.getParent.resolve(metaData.getName + "-" + metaData.getVersion + extension)
    if (!plugin.path.equals(namedPath)) {
      deleteIfExists(namedPath)
      plugin.move(namedPath)
    }

    plugin
  }

  /**
    * Starts the construction process of a [[Project]].
    *
    * @param plugin First version file
    * @return       PendingProject instance
    */
  def startProject(plugin: PluginFile): PendingProject = {
    val metaData = checkMeta(plugin)
    val owner = plugin.user

    // Start a new pending project
    val project = Project.Builder(this.service)
      .pluginId(metaData.getId)
      .ownerName(owner.name)
      .ownerId(owner.id.get)
      .name(metaData.getName)
      .build()

    val pendingProject = PendingProject(
      projects = this.projects,
      factory = this,
      underlying = project,
      file = plugin,
      config = this.config,
      channelName = this.config.getSuggestedNameForVersion(metaData.getVersion),
      cacheApi = this.cacheApi)
    pendingProject
  }

  /**
    * Starts the construction process of a [[Version]].
    *
    * @param plugin   Plugin file
    * @param project  Parent project
    * @return         PendingVersion instance
    */
  def startVersion(plugin: PluginFile, project: Project, channelName: String): PendingVersion = {
    val metaData = checkMeta(plugin)
    if (!metaData.getId.equals(project.pluginId))
      throw InvalidPluginFileException("invalid plugin ID for new version")

    // Create new pending version
    val depends = for (depend <- metaData.getRequiredDependencies.asScala) yield
      depend.getId + ":" + depend.getVersion
    val path = plugin.path
    val version = Version.Builder(this.service)
      .versionString(metaData.getVersion)
      .mcversion(metaData.getMinecraftVersion)
      .dependencyIds(depends.toList)
      .description(metaData.getDescription)
      .projectId(project.id.getOrElse(-1)) // Version might be for an uncreated project
      .fileSize(path.toFile.length)
      .hash(plugin.md5)
      .fileName(path.getFileName.toString)
      .build()

    PendingVersion(
      projects = this.projects,
      factory = this,
      project = project,
      channelName = channelName,
      channelColor = this.config.defaultChannelColor,
      underlying = version,
      plugin = plugin,
      cacheApi = this.cacheApi)
  }

  private def checkMeta(plugin: PluginFile): PluginMetadata
  = plugin.meta.getOrElse(throw new IllegalStateException("plugin metadata not loaded?"))

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
    val project = pending.underlying
    checkArgument(!this.projects.exists(project), "project already exists", "")
    checkArgument(this.projects.isNamespaceAvailable(project.ownerName, project.slug), "slug not available", "")
    checkArgument(this.config.isValidProjectName(pending.underlying.name), "invalid name", "")
    val newProject = this.projects.add(pending.underlying)

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

    this.forums.await(this.forums.createProjectTopic(newProject))

    newProject
  }

  /**
    * Creates a new release channel for the specified [[Project]].
    *
    * @param project  Project to create channel for
    * @param name     Channel name
    * @param color    Channel color
    * @return         New channel
    */
  def createChannel(project: Project, name: String, color: Color): Channel = {
    checkNotNull(project, "null project", "")
    checkArgument(project.isDefined, "undefined project", "")
    checkNotNull(name, "null name", "")
    checkArgument(this.config.isValidChannelName(name), "invalid name", "")
    checkNotNull(color, "null color", "")
    checkState(project.channels.size < this.config.projects.getInt("max-channels").get, "channel limit reached", "")
    this.service.access[Channel](classOf[Channel]).add(new Channel(name, color, project.id.get))
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
      case None =>
        channel = createChannel(project, pending.channelName, pending.channelColor)
      case Some(existing) =>
        channel = existing
    }

    // Create version
    val pendingVersion = pending.underlying
    if (pendingVersion.exists && this.config.projects.getBoolean("file-validate").get)
      throw new IllegalArgumentException("Version already exists.")

    val newVersion = this.service.access[Version](classOf[Version]).add(Version(
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
                                  override val forums: OreDiscourseApi,
                                  override val cacheApi: CacheApi,
                                  override val messages: MessagesApi,
                                  override val actorSystem: ActorSystem)
                                  extends ProjectFactory
