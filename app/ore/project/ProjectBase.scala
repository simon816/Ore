package ore.project

import java.nio.file.Files
import java.text.MessageFormat
import javax.inject.Inject

import _root_.util.StringUtils._
import _root_.util.{OreConfig, OreEnv}
import db.{ModelAccessible, ModelService}
import db.action.ModelAccess
import db.impl.OrePostgresDriver.api._
import db.impl.ProjectTable
import models.project.Project
import ore.project.util.{PendingProject, PluginFile}
import play.api.cache.CacheApi

trait ProjectBase extends ModelAccessible[ProjectTable, Project] {

  val service: ModelService
  val config: OreConfig
  val env: OreEnv
  val cacheApi: CacheApi

  def withName(owner: String, name: String): Option[Project]
  = this.find(p => p.ownerName.toLowerCase === owner.toLowerCase && p.name.toLowerCase === name.toLowerCase)

  def exists(project: Project): Boolean = this.withName(project.ownerName, project.name).isDefined

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
    * Returns the string to fill the specified Project's forum topic content
    * with.
    *
    * @param project  Project of topic
    * @return         Topic content string
    */
  def topicContentFor(project: Project): String = {
    val template = new String(Files.readAllBytes(env.conf.resolve("discourse/project_topic.md")))
    val url = config.app.getString("baseUrl").get + '/' + project.ownerName + '/' + project.slug
    MessageFormat.format(template, project.name, url, project.homePage.contents)
  }

  /**
    * Returns true if the Project's desired slug is available.
    *
    * @return True if slug is available
    */
  def isNamespaceAvailable(owner: String, slug: String): Boolean = withSlug(owner, slug).isEmpty

  /**
    * Marks the specified Project as pending for later use.
    *
    * @param project        Project that is pending
    * @param firstVersion   Uploaded plugin
    */
  def setPending(project: Project, firstVersion: PluginFile): PendingProject =  {
    val pendingProject = PendingProject(project, firstVersion)
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

}

class OreProjectBase @Inject()(override val config: OreConfig,
                               override val env: OreEnv,
                               override val cacheApi: CacheApi,
                               override val service: ModelService,
                               override val modelClass: Class[Project] = classOf[Project]) extends ProjectBase

