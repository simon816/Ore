package ore.project

import javax.inject.Inject

import _root_.util.StringUtils._
import _root_.util.{OreConfig, OreEnv}
import db.impl.OrePostgresDriver.api._
import db.impl.ProjectTable
import db.{ModelAccessible, ModelService}
import models.project.Project
import play.api.cache.CacheApi

trait ProjectBase extends ModelAccessible[ProjectTable, Project] {

  val service: ModelService
  val config: OreConfig
  val env: OreEnv
  val cacheApi: CacheApi

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
    * Returns true if the specified project exists.
    *
    * @param project  Project to check
    * @return         True if exists
    */
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
    * Returns true if the Project's desired slug is available.
    *
    * @return True if slug is available
    */
  def isNamespaceAvailable(owner: String, slug: String): Boolean = withSlug(owner, slug).isEmpty

}

class OreProjectBase @Inject()(override val config: OreConfig,
                               override val env: OreEnv,
                               override val cacheApi: CacheApi,
                               override val service: ModelService,
                               override val modelClass: Class[Project] = classOf[Project]) extends ProjectBase
