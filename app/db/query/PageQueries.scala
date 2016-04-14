package db.query

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.PagesTable
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageQueries extends Queries[PagesTable, Page](TableQuery(tag => new PagesTable(tag))) {

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], page: Page): Page = {
    page.copy(id = id, createdAt = theTime)
  }

  override def named(page: Page): Future[Option[Page]] = {
    ?(p => p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase)
  }

}
