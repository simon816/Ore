package ore.project.util

import models.project.{Channel, Project, Version}
import models.user.ProjectRole
import play.api.cache.CacheApi
import util.{Cacheable, OreConfig, PendingAction}

import scala.util.Try

/**
  * Represents a Project with an uploaded plugin that has not yet been
  * created.
  *
  * @param project  Pending project
  * @param file     Uploaded plugin
  */
case class PendingProject(manager: ProjectManager,
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
    factory.setVersionPending(project.ownerName, project.slug,
      Channel.getSuggestedNameForVersion(version.versionString), version, this.file)
  }

  override def complete: Try[Project] = Try {
    free()
    val newProject = factory.createProject(this).get
    val newVersion = factory.createVersion(this.pendingVersion).get
    newProject.recommendedVersion = newVersion
    newProject
  }

  override def cancel() = {
    free()
    this.file.delete()
    if (this.project.isDefined) this.manager.deleteProject(this.project)
  }

  override def key: String = this.project.ownerName + '/' + this.project.slug

}
