package db.query

import db.OrePostgresDriver.api._
import Queries._
import db.{ProjectTable, ProjectViewsTable, StarredProjectsTable}
import models.auth.User
import models.project.Project
import slick.lifted.Query

import scala.concurrent.Future

/**
  * Project related queries
  */
object ProjectQueries extends ModelQueries[ProjectTable, Project] {

  private val views = TableQuery[ProjectViewsTable]
  private val stars = TableQuery[StarredProjectsTable]

  /**
    * Filters projects based on the given criteria.
    *
    * @param categories   Categories of Projects
    * @param limit        Amount of Projects to get
    * @param offset       Result set offset
    * @return             Projects matching criteria
    */
  def collect(categories: Array[Int] = null, limit: Int = -1, offset: Int = -1): Future[Seq[Project]] = {
    var query: Query[ProjectTable, Project, Seq] = q[ProjectTable](classOf[Project])
    if (categories != null) {
      query = for {
        project <- query
        if project.categoryId inSetBind categories
      } yield project
    }
    if (offset > -1) {
      query = query.drop(offset)
    }
    if (limit > -1) {
      query = query.take(limit)
    }
    DB.run(query.result)
  }

  /**
    * Returns all Projects by the specified User.
    *
    * @param ownerName  Project owner
    * @return           Projects by owner
    */
  def by(ownerName: String): Future[Seq[Project]] = {
    filter[ProjectTable, Project](classOf[Project], p => p.ownerName === ownerName)
  }

  /**
    * Returns the Project with the specified name, if any.
    *
    * @param owner  Project owner
    * @param name   Project name
    * @return       Project if any, None otherwise
    */
  def withName(owner: String, name: String): Future[Option[Project]] = {
    find[ProjectTable, Project](classOf[Project], p => p.name === name && p.ownerName === owner)
  }

  /**
    * Returns the Project with the specified plugin ID, if any.
    *
    * @param pluginId   Project plugin ID
    * @return           Project if any, None otherwise
    */
  def withPluginId(pluginId: String): Future[Option[Project]] = {
    find[ProjectTable, Project](classOf[Project], p => p.pluginId === pluginId)
  }

  /**
    * Returns the Project with the specified slug, if any.
    *
    * @param slug   Project slug
    * @return       Project if any, None otherwise
    */
  def withSlug(owner: String, slug: String): Future[Option[Project]] = {
    find[ProjectTable, Project](classOf[Project], p => p.ownerName === owner && p.slug.toLowerCase === slug.toLowerCase)
  }

  /**
    * Returns the Project with the specified ID, if any.
    *
    * @param id   Project ID
    * @return     Project if any, None otherwise
    */
  def withId(id: Int): Future[Option[Project]] = find[ProjectTable, Project](classOf[Project], p => p.id === id)

  /**
    * Creates the specified Project.
    *
    * @param project  Project to create
    * @return         Newly created Project
    */
  def create(project: Project): Future[Project] = {
    // copy new vals into old project
    project.onCreate()
    val projects = q[ProjectTable](classOf[Project])
    val query = {
      projects returning projects.map(_.id) into {
        case (p, id) =>
          p.copy(id=Some(id))
      } += project
    }
    DB.run(query)
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
    val query = views.filter(pv => pv.projectId === projectId && pv.cookie === cookie).size > 0
    DB.run(query.result)
  }

  /**
    * Sets whether the specified Project has been viewed by a client with
    * the specified cookie.
    *
    * @param projectId  Project to check
    * @param cookie Cookie to look for
    */
  def setViewedBy(projectId: Int, cookie: String): Future[Any] = {
    val query = views += (None, Some(cookie), None, projectId)
    DB.run(query)
  }

  /**
    * Returns true if the specified Project has been viewed by the specified
    * User.
    *
    * @param projectId  Project to check
    * @param user User to look for
    * @return         True if user is found
    */
  def hasBeenViewedBy(projectId: Int, user: User): Future[Boolean] = {
    val query = views.filter(pv => pv.projectId === projectId && pv.userId === user.externalId).size > 0
    DB.run(query.result)
  }

  /**
    * Sets whether the specified Project has been viewed by the specified User.
    *
    * @param projectId  Project to check
    * @param user       User to look for
    * @return           True if user is found
    */
  def setViewedBy(projectId: Int, user: User) = {
    val query = views += (None, None, Some(user.externalId), projectId)
    DB.run(query)
  }

  /**
    * Returns true if the specified Project is starred by the specified User.
    *
    * @param projectId  Project to check
    * @param user       User to look for
    * @return           True if Project is starred by user
    */
  def isStarredBy(projectId: Int, user: User): Future[Boolean] = {
    val query = stars.filter(sp => sp.userId === user.externalId
      && sp.projectId === projectId).size > 0
    DB.run(query.result)
  }

  /**
    * Sets the specified Project as starred for the specified user.
    *
    * @param projectId  Project to star
    * @param user       User to star for
    */
  def starFor(projectId: Int, user: User): Future[Any] = {
    val query = stars += (user.externalId, projectId)
    DB.run(query)
  }

  /**
    * Sets the specified Project as unstarred for the specified user.
    *
    * @param projectId  Project to unstar
    * @param user       User to unstar for
    */
  def unstarFor(projectId: Int, user: User) = {
    val query = stars.filter(sp => sp.userId === user.externalId
      && sp.projectId === projectId).delete
    DB.run(query)
  }

}
