package db.query

import db._
import db.dao.ModelFilter
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries._
import models.project._
import ore.project.Categories.Category
import ore.project.ProjectMember
import ore.project.ProjectSortingStrategies.ProjectSortingStrategy

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Project related queries
  */
class ProjectQueries extends ModelQueries {

  override type Row = Project
  override type Table = ProjectTable

  val Flags = new FlagQueries

  private val views = TableQuery[ProjectViewsTable]
  private val stars = TableQuery[ProjectStarsTable]

  override val modelClass = classOf[Project]
  override val baseQuery = TableQuery[ProjectTable]

  registerModel()

  /**
    * Returns the [[ProjectMember]]s in the specified Project, sorted by role.
    *
    * @param project Project to get Members for
    * @return List of Members
    */
  def getMembers(project: Project): Future[List[ProjectMember]] = {
    // TODO: handle this differently?
    val promise = Promise[List[ProjectMember]]
    ModelQueries.Users.ProjectRoles.distinctUsersIn(project.id.get).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(users) =>
        val members = for (user <- users) yield new ProjectMember(project, user.username)
        promise.success(members.toList.sorted.reverse)
    }
    promise.future
  }

  /**
    * Returns a ModelFilter to match a string query
    *
    * @param query String query
    * @return Project's matching query
    */
  def searchFilter(query: String): ModelFilter[ProjectTable, Project] = {
    val q = '%' + query.toLowerCase + '%'
    ModelFilter { p =>
      (p.name.toLowerCase like q) || (p.description.toLowerCase like q) || (p.ownerName.toLowerCase like q)
    }
  }

  /**
    * Filters projects by owner name.
    *
    * @param ownerName Owner name
    * @return Project filter
    */
  def ownerFilter(ownerName: String): ModelFilter[ProjectTable, Project]
  = ModelFilter(p => p.ownerName.toLowerCase === ownerName.toLowerCase)

  /**
    * Filters projects based on the given criteria.
    *
    * @param filter Filter to match Projects on
    * @param limit  Amount of Projects to get
    * @param offset Result set offset
    * @return Projects matching criteria
    */
  def collect(filter: ProjectTable => Rep[Boolean], sort: ProjectSortingStrategy,
              limit: Int, offset: Int): Future[Seq[Project]]
  = collect(limit, offset, filter, Option(sort).map(_.fn).orNull)

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
  def collect(filter: ProjectTable => Rep[Boolean], categories: Array[Category],
              limit: Int, offset: Int, sort: ProjectSortingStrategy): Future[Seq[Project]] = {
    val f: ProjectTable => Rep[Boolean] = if (categories != null) {
      val cf: ProjectTable => Rep[Boolean] = p => p.category inSetBind categories
      if (filter != null) p => cf(p) && filter(p) else cf
    } else filter
    collect(f, sort, limit, offset)
  }

  /**
    * Returns true if the specified Project has been viewed by a client with
    * the specified cookie.
    *
    * @param projectId Project to check
    * @param cookie    Cookie to look for
    * @return True if cookie is found
    */
  def hasBeenViewedBy(projectId: Int, cookie: String): Future[Boolean] = run((this.views.filter { pv =>
    pv.projectId === projectId && pv.cookie === cookie
  }.length > 0).result)

  /**
    * Sets whether the specified Project has been viewed by a client with
    * the specified cookie.
    *
    * @param projectId Project to check
    * @param cookie    Cookie to look for
    */
  def setViewedBy(projectId: Int, cookie: String): Future[Any]
  = run(this.views +=(None, Some(cookie), None, projectId))

  /**
    * Returns true if the specified Project has been viewed by the specified
    * User.
    *
    * @param projectId Project to check
    * @param userId    User to look for
    * @return True if user is found
    */
  def hasBeenViewedBy(projectId: Int, userId: Int): Future[Boolean] = run((this.views.filter { pv =>
    pv.projectId === projectId && pv.userId === userId
  }.length > 0).result)

  /**
    * Sets whether the specified Project has been viewed by the specified User.
    *
    * @param projectId Project to check
    * @param userId    User to look for
    * @return True if user is found
    */
  def setViewedBy(projectId: Int, userId: Int): Future[Any] = run(this.views +=(None, None, Some(userId), projectId))

  /**
    * Returns true if the specified Project is starred by the specified User.
    *
    * @param projectId Project to check
    * @param userId    User to look for
    * @return True if Project is starred by user
    */
  def isStarredBy(projectId: Int, userId: Int): Future[Boolean] = run((this.stars.filter { sp =>
    sp.userId === userId && sp.projectId === projectId
  }.length > 0).result)

  /**
    * Sets the specified Project as starred for the specified user.
    *
    * @param projectId Project to star
    * @param userId    User to star for
    */
  def starFor(projectId: Int, userId: Int): Future[Any] = run(this.stars +=(userId, projectId))

  /**
    * Sets the specified Project as unstarred for the specified user.
    *
    * @param projectId Project to unstar
    * @param userId    User to unstar for
    */
  def unstarFor(projectId: Int, userId: Int) = run(this.stars.filter { sp =>
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
    run(starQuery.result).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(userStars) =>
        val projectIds = userStars.map(_._2)
        var projectsQuery = this.baseQuery.filter(_.id inSetBind projectIds)
        if (offset > -1) projectsQuery = projectsQuery.drop(offset)
        if (limit > -1) projectsQuery = projectsQuery.take(limit)
        promise.completeWith(run(projectsQuery.result))
    }
    promise.future
  }

  override def like(model: Project) = find { p =>
    p.ownerName.toLowerCase === model.ownerName.toLowerCase && p.name.toLowerCase === model.name.toLowerCase
  }

}
