package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.PagesTable
import db.query.Queries._
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

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], page: Page): Page = {
    page.copy(id = id, createdAt = theTime)
  }

  override def named(page: Page): Future[Option[Page]] = withName(page.projectId, page.name)

}
