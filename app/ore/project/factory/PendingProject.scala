package ore.project.factory

import db.impl.access.ProjectBase
import models.project.Project
import ore.project.io.PluginFile
import models.user.role.ProjectRole
import ore.{Cacheable, OreConfig}
import play.api.cache.CacheApi
import util.PendingAction

import scala.util.Try

/**
  * Represents a Project with an uploaded plugin that has not yet been
  * created.
  *
  * @param project  Pending project
  * @param file     Uploaded plugin
  */
case class PendingProject(projects: ProjectBase,
                          factory: ProjectFactory,
                          project: Project,
                          file: PluginFile,
                          implicit val config: OreConfig,
                          var roles: Set[ProjectRole] = Set(),
                          override val cacheApi: CacheApi)
                          extends PendingAction[Project]
                            with Cacheable {

  /**
    * The first [[PendingVersion]] for this PendingProject.
    */
  val pendingVersion: PendingVersion = {
    val version = this.factory.versionFromFile(this.project, this.file)
    this.factory.setVersionPending(this.project, this.config.getSuggestedNameForVersion(version.versionString), version,
      this.file)
  }

  override def complete: Try[Project] = Try {
    free()
    val newProject = this.factory.createProject(this).get
    this.pendingVersion.project = newProject
    val newVersion = this.factory.createVersion(this.pendingVersion).get
    newProject.recommendedVersion = newVersion
    newProject
  }

  override def cancel() = {
    free()
    this.file.delete()
    if (this.project.isDefined)
      this.projects.delete(this.project)
  }

  override def key: String = this.project.ownerName + '/' + this.project.slug

}
