package ore.project.factory

import scala.concurrent.ExecutionContext

import play.api.cache.SyncCacheApi

import db.access.ModelView
import db.{Model, DbRef, ModelService}
import db.impl.schema.VersionTable
import db.impl.OrePostgresDriver.api._
import models.project._
import models.user.User
import ore.project.Dependency
import ore.project.io.PluginFileWithData
import ore.{Cacheable, Color, Platform}

import cats.effect.{ContextShift, IO}
import cats.syntax.all._
import slick.lifted.TableQuery

/**
  * Represents a pending version to be created later.
  *
  * @param channelName    Name of channel this version will be in
  * @param channelColor   Color of channel for this version
  * @param plugin         Uploaded plugin
  */
case class PendingVersion(
    versionString: String,
    dependencyIds: List[String],
    description: Option[String],
    projectId: Option[DbRef[Project]], // Version might be for an uncreated project
    fileSize: Long,
    hash: String,
    fileName: String,
    signatureFileName: String,
    authorId: DbRef[User],
    projectUrl: String,
    channelName: String,
    channelColor: Color,
    plugin: PluginFileWithData,
    createForumPost: Boolean,
    cacheApi: SyncCacheApi
) extends Cacheable {

  def complete(
      project: Model[Project],
      factory: ProjectFactory
  )(
      implicit ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[(Model[Version], Model[Channel], Seq[Model[VersionTag]])] =
    free *> factory.createVersion(project, this)

  override def key: String = projectUrl + '/' + versionString

  def dependencies: List[Dependency] =
    for (depend <- dependencyIds) yield {
      val data = depend.split(":", 2)
      Dependency(data(0), if (data.length > 1) data(1) else "")
    }

  def dependenciesAsGhostTags: Seq[VersionTag] =
    Platform.ghostTags(-1L, dependencies)

  /**
    * Returns true if a project ID is defined on this Model, there is no
    * matching hash in the Project, and there is no duplicate version with
    * the same name in the Project.
    *
    * @return True if exists
    */
  def exists(implicit service: ModelService): IO[Boolean] = {
    val hashExistsBaseQuery = for {
      v <- TableQuery[VersionTable]
      if v.projectId === projectId
      if v.hash === hash
    } yield v.id

    val hashExistsQuery = hashExistsBaseQuery.exists

    projectId.fold(IO.pure(false)) { projectId =>
      for {
        project <- ModelView.now(Project).get(projectId).getOrElse(sys.error(s"No project found for id $projectId"))
        versionExistsQuery = project
          .versions(ModelView.later(Version))
          .exists(_.versionString.toLowerCase === this.versionString.toLowerCase)
        res <- service.runDBIO(Query((hashExistsQuery, versionExistsQuery)).map(t => t._1 && t._2).result.head)
      } yield res
    }
  }

  def asVersion(projectId: DbRef[Project], channelId: DbRef[Channel]): Version = Version(
    versionString = versionString,
    dependencyIds = dependencyIds,
    description = description,
    projectId = projectId,
    channelId = channelId,
    fileSize = fileSize,
    hash = hash,
    authorId = authorId,
    fileName = fileName,
    signatureFileName = signatureFileName
  )
}
