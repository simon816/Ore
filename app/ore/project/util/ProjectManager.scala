package ore.project.util

import java.nio.file.Files
import javax.inject.Inject

import com.google.common.base.Preconditions._
import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.service.{ProjectBase, UserBase}
import forums.DiscourseApi
import models.project.{Channel, Project, Version}
import org.apache.commons.io.FileUtils
import util.{OreConfig, OreEnv}
import util.StringUtils._

/**
  * Handles management tasks for [[models.project.Project]]s and their components.
  */
trait ProjectManager {

  val service: ModelService
  val env: OreEnv
  val fileManager: ProjectFileManager = new ProjectFileManager(this.env)

  implicit val users: UserBase = this.service.access(classOf[UserBase])
  implicit val projects: ProjectBase = this.service.access(classOf[ProjectBase])

  implicit val config: OreConfig
  implicit val forums: DiscourseApi

  /**
    * Renames the specified [[Project]].
    *
    * @param project  Project to rename
    * @param name     New name to assign Project
    */
  def renameProject(project: Project, name: String) = {
    val newName = compact(name)
    val newSlug = slugify(newName)
    checkArgument(this.config.isValidProjectName(name), "invalid name", "")
    checkArgument(this.projects.isNamespaceAvailable(project.ownerName, newSlug), "slug not available", "")

    this.fileManager.renameProject(project.ownerName, project.name, newName)
    project.name = newName
    project.slug = newSlug

    if (project.topicId.isDefined) {
      this.forums.embed.renameTopic(project)
      this.forums.embed.updateTopic(project)
    }
  }

  /**
    * Irreversibly deletes this project.
    *
    * @param project Project to delete
    */
  def deleteProject(project: Project) = {
    FileUtils.deleteDirectory(this.fileManager.projectDir(project.ownerName, project.name).toFile)
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
    checkArgument(proj.id.get == channel.projectId, "invalid project id", "")

    val channels = proj.channels.all
    checkArgument(channels.size > 1, "only one channel", "")
    checkArgument(channel.versions.isEmpty || channels.count(c => c.versions.nonEmpty) > 1, "last non-empty channel", "")

    FileUtils.deleteDirectory(this.fileManager.projectDir(proj.ownerName, proj.name).resolve(channel.name).toFile)
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

    Files.delete(this.fileManager.uploadPath(proj.ownerName, proj.name, version.versionString))
    version.remove()
  }

}

case class OreProjectManager @Inject()(override val service: ModelService,
                                       override val env: OreEnv,
                                       override val config: OreConfig,
                                       override val forums: DiscourseApi)
                                       extends ProjectManager
