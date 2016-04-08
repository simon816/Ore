package db.query

import db.OrePostgresDriver.api._
import Queries._
import db.PagesTable
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
object PageQueries extends ModelQueries[PagesTable, Page] {

  /**
    * Returns all Pages in the specified Project.
    *
    * @param projectId  Project to get Pages for
    * @return           Pages in Project
    */
  def in(projectId: Int): Future[Seq[Page]] = {
    filter[PagesTable, Page](classOf[Page], p => p.projectId === projectId)
  }

  /**
    * Returns the Page with the specified name in the specified Project.
    *
    * @param projectId  Project with Page
    * @param name       Page name
    * @return           Page with name
    */
  def withName(projectId: Int, name: String): Future[Option[Page]] = {
    find[PagesTable, Page](classOf[Page], p => p.projectId === projectId
      && p.name.toLowerCase === name.toLowerCase)
  }

  /**
    * Returns the specified Page or creates it if it doesn't exist.
    *
    * @param page   Page to get or create
    * @return       Existing or newly created Page
    */
  def getOrCreate(page: Page): Page = {
    now(withName(page.projectId, page.name)).get.getOrElse(now(create(page)).get)
  }

  /**
    * Creates the specified Page.
    *
    * @param page   Page to create
    * @return       Newly created Page
    */
  def create(page: Page) = {
    page.onCreate()
    val pages = q[PagesTable](classOf[Page])
    val query = {
      pages returning pages.map(_.id) into {
        case (p, id) =>
          p.copy(id=Some(id))
      } += page
    }
    DB.run(query)
  }

}
