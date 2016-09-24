package db.impl.schema

import db.ModelAction.wrapSeq
import db._
import db.impl._
import db.impl.access.UserBase
import OrePostgresDriver.api._
import models.project._
import models.statistic.ProjectView
import models.user.User
import ore.project.Categories.Category
import ore.project.ProjectSortingStrategies.ProjectSortingStrategy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Project related queries
  */
class ProjectSchema(override val service: ModelService)
  extends ModelSchema[Project](service, classOf[Project], TableQuery[ProjectTable]) {

  /** The [[ModelSchema]] for [[Flag]]s. */
  val FlagActions = service.registry.registerSchema(
    new ModelSchema[Flag](this.service, classOf[Flag], TableQuery[FlagTable])
  )

  case object ViewSchema extends ModelSchema[ProjectView](
    this.service, classOf[ProjectView], TableQuery[ProjectViewsTable])
    with StatSchema[ProjectView]

  /** The [[ModelSchema]] for [[ProjectView]]. */
  val ViewActions = service.registry.registerSchema(ViewSchema)

  implicit private val users: UserBase = this.service.getModelBase(classOf[UserBase])
  private val stars = TableQuery[ProjectStarsTable]

  /**
    * Returns all [[User]]s with at least one [[Project]].
    *
    * @return Project authors
    */
  def distinctAuthors: Future[Seq[User]] = {
    service.DB.db.run {
      (for (project <- this.baseQuery) yield project.userId).distinct.result
    } map { userIds =>
      userIds.map(this.users.get(_).get)
    }
  }

  /**
    * Returns a ModelFilter to match a string query
    *
    * @param query String query
    * @return Project's matching query
    */
  def searchFilter(query: String): ModelFilter[Project] = {
    val q = '%' + query.toLowerCase + '%'
    ModelFilter[Project] { p =>
      (p.name.toLowerCase like q) ||
        (p.description.toLowerCase like q) ||
        (p.ownerName.toLowerCase like q) ||
        (p.pluginId.toLowerCase like q)
    }
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param filter Filter to match Projects on
    * @param limit  Amount of Projects to get
    * @param offset Result set offset
    * @return Projects matching criteria
    */
  def collect(filter: Project#T => Rep[Boolean], sort: ProjectSortingStrategy,
              limit: Int, offset: Int): Future[Seq[Project]]
  = this.service.collect[Project](this.modelClass, filter, Option(sort).map(_.fn).orNull, limit, offset)

  /**
    * Filters projects based on the given criteria.
    *
    * @param filter     Model filter
    * @param categories Project categories
    * @param limit      Amount to take
    * @param offset     Amount to drop
    * @param sort       Ordering
    * @return Filtered projects
    */
  def collect(filter: Project#T => Rep[Boolean], categories: Array[Category],
              limit: Int, offset: Int, sort: ProjectSortingStrategy): Future[Seq[Project]] = {
    val f: ProjectTable => Rep[Boolean] = {
      if (categories != null) {
        val cf: ProjectTable => Rep[Boolean] = p => p.category inSetBind categories
        if (filter != null)
          p => cf(p) && filter(p)
        else
          cf
      } else filter
    }
    collect(f, sort, limit, offset)
  }

  /**
    * Returns true if the specified Project is starred by the specified User.
    *
    * @param projectId Project to check
    * @param userId    User to look for
    * @return True if Project is starred by user
    */
  def isStarredBy(projectId: Int, userId: Int): Future[Boolean] = service.DB.db.run((this.stars.filter { sp =>
    sp.userId === userId && sp.projectId === projectId
  }.length > 0).result)

  /**
    * Sets the specified Project as starred for the specified user.
    *
    * @param projectId Project to star
    * @param userId    User to star for
    */
  def starFor(projectId: Int, userId: Int): Future[Any] = service.DB.db.run(this.stars +=(userId, projectId))

  /**
    * Sets the specified Project as unstarred for the specified user.
    *
    * @param projectId Project to unstar
    * @param userId    User to unstar for
    */
  def unstarFor(projectId: Int, userId: Int) = service.DB.db.run(this.stars.filter { sp =>
    sp.userId === userId && sp.projectId === projectId
  }.delete)

  /**
    * Returns all the Projects that the specified User has starred.
    *
    * @param userId   User ID to get stars for
    * @param limit    Max amount of stars to retrieve
    * @param offset   Amount of stars to skip
    * @return         Projects starred by user
    */
  def starredBy(userId: Int, limit: Int = -1, offset: Int = -1): Future[Seq[Project]] = {
    val promise = Promise[Seq[Project]]
    val starQuery = this.stars.filter(_.userId === userId)
    service.DB.db.run(starQuery.result).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(userStars) =>
        val projectIds = userStars.map(_._2)
        var projectsQuery = this.baseQuery.filter(_.id inSetBind projectIds)
        if (offset > -1) projectsQuery = projectsQuery.drop(offset)
        if (limit > -1) projectsQuery = projectsQuery.take(limit)
        promise.completeWith(service.doAction(projectsQuery.result))
    }
    promise.future
  }

  override def like(model: Project) = {
    this.service.find[Project](this.modelClass, p => p.ownerName.toLowerCase === model.ownerName.toLowerCase
      && p.name.toLowerCase === model.name.toLowerCase)
  }

}
