package db.impl.access

import _root_.util.StringUtils._
import db.impl.ProjectTable
import db.impl.pg.OrePostgresDriver.api._
import db.{ModelBase, ModelService}
import models.project.Project

class ProjectBase(override val service: ModelService) extends ModelBase[ProjectTable, Project] {

  override val modelClass = classOf[Project]

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

}
