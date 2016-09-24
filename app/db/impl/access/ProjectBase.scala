package db.impl.access

import java.nio.file.Files
import java.nio.file.Files._

import _root_.util.StringUtils._
import com.google.common.base.Preconditions._
import db.impl.OrePostgresDriver.api._
import db.{ModelBase, ModelService}
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import ore.project.io.ProjectFileManager
import ore.{OreConfig, OreEnv}
import org.apache.commons.io.FileUtils

class ProjectBase(override val service: ModelService,
                  env: OreEnv,
                  config: OreConfig,
                  forums: DiscourseApi)
                  extends ModelBase[Project] {

  override val modelClass = classOf[Project]

  val fileManager = new ProjectFileManager(this.env)

  implicit val self = this

  /**
    * Returns the Project with the specified owner name and name.
    *
    * @param owner  Owner name
    * @param name   Project name
    * @return       Project with name
    */
  def withName(owner: String, name: String): Option[Project]
  = this.find(p => p.ownerName.toLowerCase === owner.toLowerCase && p.name.toLowerCase === name.toLowerCase)

  /**
    * Returns the Project with the specified owner name and URL slug, if any.
    *
    * @param owner  Owner name
    * @param slug   URL slug
    * @return       Project if found, None otherwise
    */
  def withSlug(owner: String, slug: String): Option[Project]
  = this.find(p => p.ownerName.toLowerCase === owner.toLowerCase && p.slug.toLowerCase === slug.toLowerCase)

  /**
    * Returns the Project with the specified plugin ID, if any.
    *
    * @param pluginId Plugin ID
    * @return         Project if found, None otherwise
    */
  def withPluginId(pluginId: String): Option[Project]
  = this.find(equalsIgnoreCase(_.pluginId, pluginId))

  /**
    * Returns true if the Project's desired slug is available.
    *
    * @return True if slug is available
    */
  def isNamespaceAvailable(owner: String, slug: String): Boolean = withSlug(owner, slug).isEmpty

  /**
    * Returns true if the specified project exists.
    *
    * @param project  Project to check
    * @return         True if exists
    */
  def exists(project: Project): Boolean = this.withName(project.ownerName, project.name).isDefined

  /**
    * Saves any pending icon that has been uploaded for the specified [[Project]].
    *
    * @param project Project to save icon for
    */
  def savePendingIcon(project: Project) = {
    this.fileManager.getPendingIconPath(project).foreach { iconPath =>
      val iconDir = this.fileManager.getIconDir(project.ownerName, project.name)
      if (notExists(iconDir))
        createDirectories(iconDir)
      FileUtils.cleanDirectory(iconDir.toFile)
      move(iconPath, iconDir.resolve(iconPath.getFileName))
    }
  }

  /**
    * Renames the specified [[Project]].
    *
    * @param project  Project to rename
    * @param name     New name to assign Project
    */
  def rename(project: Project, name: String) = {
    val newName = compact(name)
    val newSlug = slugify(newName)
    checkArgument(this.config.isValidProjectName(name), "invalid name", "")
    checkArgument(this.isNamespaceAvailable(project.ownerName, newSlug), "slug not available", "")

    this.fileManager.renameProject(project.ownerName, project.name, newName)
    project.name = newName
    project.slug = newSlug

    if (project.topicId.isDefined) {
      this.forums.embed.renameTopic(project)
      this.forums.embed.updateTopic(project)
    }
  }

  /**
    * Irreversibly deletes this channel and all version associated with it.
    *
    * @param context Project context
    */
  def deleteChannel(channel: Channel)(implicit context: Project = null) = {
    val proj = if (context != null) context else channel.project
    checkArgument(proj.id.get == channel.projectId, "invalid project id", "")

    val channels = proj.channels.all
    checkArgument(channels.size > 1, "only one channel", "")
    checkArgument(channel.versions.isEmpty || channels.count(c => c.versions.nonEmpty) > 1, "last non-empty channel", "")

    FileUtils.deleteDirectory(this.fileManager.getProjectDir(proj.ownerName, proj.name).resolve(channel.name).toFile)
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

    val rv = proj.recommendedVersion
    version.remove()

    // Set recommended version to latest version if the deleted version was the rv
    if (version.equals(rv)) {
      proj.recommendedVersion = proj.versions.sorted(_.createdAt.desc, limit = 1).head
    }

    // Delete channel if now empty
    val channel: Channel = version.channel
    if (channel.versions.isEmpty)
      this.deleteChannel(channel)

    Files.delete(this.fileManager.getProjectDir(proj.ownerName, proj.name).resolve(version.fileName))
  }

  /**
    * Irreversibly deletes this project.
    *
    * @param project Project to delete
    */
  def delete(project: Project) = {
    FileUtils.deleteDirectory(this.fileManager.getProjectDir(project.ownerName, project.name).toFile)
    if (project.topicId.isDefined)
      this.forums.embed.deleteTopic(project)
    project.remove()
  }

}
