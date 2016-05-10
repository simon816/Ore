package ore.project.util

import java.nio.file.Files
import javax.inject.Inject

import com.google.common.base.Preconditions._
import db.ModelService
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import models.user.{ProjectRole, User}
import ore.UserBase
import ore.permission.role.RoleTypes
import ore.project.ProjectBase
import play.api.cache.CacheApi
import play.api.libs.Files.TemporaryFile
import util.OreConfig
import util.StringUtils.equalsIgnoreCase

import scala.util.Try

/**
  * Handles creation of Project's and their components.
  */
trait ProjectFactory {

  val projects: ProjectBase
  val users: UserBase
  val fileManager: ProjectFileManager
  val config: OreConfig
  val cacheApi: CacheApi
  val env = fileManager.env

  /**
    * Initializes a new PluginFile with the specified owner and temporary file.
    *
    * @param tmp    Temporary file
    * @param owner  Project owner
    * @return       New plugin file
    */
  def initUpload(tmp: TemporaryFile, name: String, owner: User): Try[PluginFile] = Try {
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
  def setPending(project: Project, firstVersion: PluginFile): PendingProject =  {
    val pendingProject = PendingProject(cacheApi, this, project, firstVersion)
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
  def getPending(owner: String, slug: String): Option[PendingProject]
  = cacheApi.get[PendingProject](owner + '/' + slug)

  /**
    * Creates a new Project from the specified PendingProject
    *
    * @param pending  PendingProject
    * @return         New Project
    * @throws         IllegalArgumentException if the project already exists
    */
  protected[util] def createProject(pending: PendingProject)(implicit service: ModelService,
                                             forums: DiscourseApi): Try[Project] = Try {
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
  protected[util] def createVersion(pending: PendingVersion)(implicit service: ModelService): Try[Version] = Try {
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

class OreProjectFactory @Inject()(override val projects: ProjectBase,
                                  override val users: UserBase,
                                  override val fileManager: ProjectFileManager,
                                  override val config: OreConfig,
                                  override val cacheApi: CacheApi) extends ProjectFactory
