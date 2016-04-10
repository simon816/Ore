package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.query.Queries.DB.run
import db.{ProjectStarsTable, ProjectTable, ProjectViewsTable}
import models.project.Project

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, Future}
import scala.util.{Success, Failure}

/**
  * Project related queries
  */
object ProjectQueries extends Queries[ProjectTable, Project](TableQuery(tag => new ProjectTable(tag))) {

  private val views = TableQuery[ProjectViewsTable]
  private val stars = TableQuery[ProjectStarsTable]

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories   Categories of Projects
    * @param limit        Amount of Projects to get
    * @param offset       Result set offset
    * @return             Projects matching criteria
    */
  def collect(categories: Array[Int], limit: Int, offset: Int): Future[Seq[Project]] = {
    var filter: ProjectTable => Rep[Boolean] = null
    if (categories != null) {
      filter = p => p.categoryId inSetBind categories
    }
    collect(limit, offset, filter)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories   Categories of Projects
    * @param limit        Amount of Projects to get
    * @return             Projects matching criteria
    */
  def collect(categories: Array[Int], limit: Int): Future[Seq[Project]] = {
    collect(categories, limit, -1)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories   Categories of Projects
    * @return             Projects matching criteria
    */
  def collect(categories: Array[Int]): Future[Seq[Project]] = {
    collect(categories, -1, -1)
  }

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories   Categories of Projects
    * @param limit        Amount of Projects to get
    * @return             Projects matching criteria
    */
  def collect(limit: Int): Future[Seq[Project]] = {
    collect(null, limit, -1)
  }

  /**
    * Returns all Projects by the specified User.
    *
    * @param ownerName  Project owner
    * @return           Projects by owner
    */
  def by(ownerName: String): Future[Seq[Project]] = {
    run(this.models.filter(p => p.ownerName === ownerName).result)
  }

  /**
    * Returns the Project with the specified name, if any.
    *
    * @param owner  Project owner
    * @param name   Project name
    * @return       Project if any, None otherwise
    */
  def withName(owner: String, name: String): Future[Option[Project]] = {
    find(p => p.name.toLowerCase === name.toLowerCase && p.ownerName === owner)
  }

  /**
    * Returns the Project with the specified plugin ID, if any.
    *
    * @param pluginId   Project plugin ID
    * @return           Project if any, None otherwise
    */
  def withPluginId(pluginId: String): Future[Option[Project]] = {
    find(p => p.pluginId === pluginId)
  }

  /**
    * Returns the Project with the specified slug, if any.
    *
    * @param slug   Project slug
    * @return       Project if any, None otherwise
    */
  def withSlug(owner: String, slug: String): Future[Option[Project]] = {
    find(p => p.ownerName === owner && p.slug.toLowerCase === slug.toLowerCase)
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
    val query = this.views.filter(pv => pv.projectId === projectId && pv.cookie === cookie).size > 0
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
    val query = this.views.filter(pv => pv.projectId === projectId && pv.userId === userId).size > 0
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
    val query = this.stars.filter(sp => sp.userId === userId && sp.projectId === projectId).size > 0
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
    val starQuery = this.stars.filter(sp => sp.userId === userId)
    run(starQuery.result).andThen {
      case Failure(thrown) => promise.failure(thrown)
      case Success(userStars) =>
        val projectIds = userStars.map(_._2)
        var projectsQuery = this.models.filter(p => p.id inSetBind projectIds)
        if (offset > -1) projectsQuery = projectsQuery.drop(offset)
        if (limit > -1) projectsQuery = projectsQuery.take(limit)
        promise.completeWith(run(projectsQuery.result))
    }
    promise.future
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], project: Project): Project = {
    project.copy(id = id, createdAt = theTime)
  }

}
