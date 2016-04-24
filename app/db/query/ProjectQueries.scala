package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.query.Queries.DB.run
import db.{ProjectStarsTable, ProjectTable, ProjectViewsTable}
import models.project.Project
import ore.project.Categories.Category
import ore.project.ProjectSortingStrategies.ProjectSortingStrategy
import ore.project.member.Member

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * Project related queries
  */
class ProjectQueries extends Queries[ProjectTable, Project](TableQuery(tag => new ProjectTable(tag))) {

  private val views = TableQuery[ProjectViewsTable]
  private val stars = TableQuery[ProjectStarsTable]

  def searchFilter(query: String): ProjectTable => Rep[Boolean] = {
    val q = '%' + query.toLowerCase + '%'
    p => (p.name.toLowerCase like q) || (p.description.toLowerCase like q) || (p.ownerName.toLowerCase like q)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param filter Filter to match Projects on
    * @param limit  Amount of Projects to get
    * @param offset Result set offset
    * @return       Projects matching criteria
    */
  def collect(filter: ProjectTable => Rep[Boolean], sort: ProjectSortingStrategy,
              limit: Int, offset: Int): Future[Seq[Project]] = {
    collect(limit, offset, filter, Option(sort).map(_.f).orNull)
  }

  def collect(filter: ProjectTable => Rep[Boolean], limit: Int, offset: Int): Future[Seq[Project]] = {
    collect(filter, null.asInstanceOf[ProjectSortingStrategy], limit, offset)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param filter Filter to match Projects on
    * @param limit  Amount of Projects to get
    * @return       Projects matching criteria
    */
  def collect(filter: ProjectTable => Rep[Boolean], limit: Int): Future[Seq[Project]] = {
    collect(filter, limit, -1)
  }

  def collect(filter: ProjectTable => Rep[Boolean], categories: Array[Category],
              limit: Int, offset: Int, sort: ProjectSortingStrategy): Future[Seq[Project]] = {
    val f: ProjectTable => Rep[Boolean] = if (categories != null) {
      val cf: ProjectTable => Rep[Boolean] = p => p.category inSetBind categories
      if (filter != null) p => cf(p) && filter(p) else cf
    } else filter
    collect(f, sort, limit, offset)
  }

  def collect(filter: ProjectTable => Rep[Boolean], categories: Array[Category],
              limit: Int, offset: Int): Future[Seq[Project]] = {
    collect(filter, categories, limit, offset, null)
  }

  def collect(filter: ProjectTable => Rep[Boolean], categories: Array[Category], limit: Int,
              sort: ProjectSortingStrategy): Future[Seq[Project]] = {
    collect(filter, categories, limit, -1, sort)
  }

  def collect(filter: ProjectTable => Rep[Boolean], categories: Array[Category], limit: Int): Future[Seq[Project]] = {
    collect(filter, categories, limit, null)
  }

    /**
    * Filters projects based on the given criteria.
    *
    * @param categories Categories of Projects
    * @param limit      Amount of Projects to get
    * @param offset     Result set offset
    * @return           Projects matching criteria
    */
  def collect(categories: Array[Category], limit: Int, offset: Int): Future[Seq[Project]] = {
    collect(null, categories, limit, offset)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories Categories of Projects
    * @param limit      Amount of Projects to get
    * @return           Projects matching criteria
    */
  def collect(categories: Array[Category], limit: Int): Future[Seq[Project]] = {
    collect(categories, limit, -1)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories Categories of Projects
    * @return           Projects matching criteria
    */
  def collect(categories: Array[Category]): Future[Seq[Project]] = {
    collect(categories, -1)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param limit Amount of Projects to get
    * @return      Projects matching criteria
    */
  def collect(limit: Int): Future[Seq[Project]] = {
    collect(null.asInstanceOf[Array[Category]], limit)
  }

  /**
    * Returns all Projects by the specified User.
    *
    * @param ownerName  Project owner
    * @return           Projects by owner
    */
  def by(ownerName: String): Future[Seq[Project]] = {
    run(this.models.filter(_.ownerName === ownerName).result)
  }

  /**
    * Returns the Project with the specified name, if any.
    *
    * @param owner  Project owner
    * @param name   Project name
    * @return       Project if any, None otherwise
    */
  def withName(owner: String, name: String): Future[Option[Project]] = {
    ?(p => p.name.toLowerCase === name.toLowerCase && p.ownerName === owner)
  }

  /**
    * Returns the Project with the specified plugin ID, if any.
    *
    * @param pluginId   Project plugin ID
    * @return           Project if any, None otherwise
    */
  def withPluginId(pluginId: String): Future[Option[Project]] = {
    ?(_.pluginId === pluginId)
  }

  /**
    * Returns the Project with the specified slug, if any.
    *
    * @param slug   Project slug
    * @return       Project if any, None otherwise
    */
  def withSlug(owner: String, slug: String): Future[Option[Project]] = {
    ?(p => p.ownerName === owner && p.slug.toLowerCase === slug.toLowerCase)
  }

  /**
    * Sets the category for the specified Project.
    *
    * @param project  Project to update
    * @param category Category to set
    */
  def setCategory(project: Project, category: Category) = {
    val query = for { model <- this.models if model.id === project.id.get } yield model.category
    run(query.update(category))
  }

  /**
    * Returns true if the specified Project has been viewed by a client with
    * the specified cookie.
    *
    * @param projectId  Project to check
    * @param cookie Cookie to look for
    * @return         True if cookie is found
    */
  def hasBeenViewedBy(projectId: Int, cookie: String): Future[Boolean] = {
    val query = this.views.filter(pv => pv.projectId === projectId && pv.cookie === cookie).length > 0
    run(query.result)
  }

  /**
    * Sets whether the specified Project has been viewed by a client with
    * the specified cookie.
    *
    * @param projectId  Project to check
    * @param cookie Cookie to look for
    */
  def setViewedBy(projectId: Int, cookie: String): Future[Any] = {
    val query = this.views += (None, Some(cookie), None, projectId)
    run(query)
  }

  /**
    * Returns true if the specified Project has been viewed by the specified
    * User.
    *
    * @param projectId  Project to check
    * @param userId     User to look for
    * @return           True if user is found
    */
  def hasBeenViewedBy(projectId: Int, userId: Int): Future[Boolean] = {
    val query = this.views.filter(pv => pv.projectId === projectId && pv.userId === userId).length > 0
    run(query.result)
  }

  /**
    * Sets whether the specified Project has been viewed by the specified User.
    *
    * @param projectId  Project to check
    * @param userId     User to look for
    * @return           True if user is found
    */
  def setViewedBy(projectId: Int, userId: Int): Future[Any] = {
    val query = this.views += (None, None, Some(userId), projectId)
    run(query)
  }

  /**
    * Returns true if the specified Project is starred by the specified User.
    *
    * @param projectId  Project to check
    * @param userId     User to look for
    * @return           True if Project is starred by user
    */
  def isStarredBy(projectId: Int, userId: Int): Future[Boolean] = {
    val query = this.stars.filter(sp => sp.userId === userId && sp.projectId === projectId).length > 0
    run(query.result)
  }

  /**
    * Sets the specified Project as starred for the specified user.
    *
    * @param projectId  Project to star
    * @param userId     User to star for
    */
  def starFor(projectId: Int, userId: Int): Future[Any] = {
    val query = this.stars += (userId, projectId)
    run(query)
  }

  /**
    * Sets the specified Project as unstarred for the specified user.
    *
    * @param projectId  Project to unstar
    * @param userId     User to unstar for
    */
  def unstarFor(projectId: Int, userId: Int) = {
    val query = this.stars.filter(sp => sp.userId === userId && sp.projectId === projectId).delete
    run(query)
  }

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
        var projectsQuery = this.models.filter(_.id inSetBind projectIds)
        if (offset > -1) projectsQuery = projectsQuery.drop(offset)
        if (limit > -1) projectsQuery = projectsQuery.take(limit)
        promise.completeWith(run(projectsQuery.result))
    }
    promise.future
  }

  /**
    * Returns the [[Member]]s in the specified Project, sorted by role.
    *
    * @param project  Project to get Members for
    * @return         List of Members
    */
  def members(project: Project): Future[List[Member]] = {
    val promise = Promise[List[Member]]
    Queries.Users.ProjectRoles.distinctUsersIn(project.id.get).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(users) =>
        val members = for (user <- users) yield new Member(project, user.username)
        promise.success(members.toList.sorted.reverse)
    }
    promise.future
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], project: Project): Project = {
    project.copy(id = id, createdAt = theTime)
  }

}
