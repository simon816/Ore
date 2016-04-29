package db.query

import db.OrePostgresDriver.api._
import db.PageTable
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageQueries extends ModelQueries {

  override type Row = Page
  override type Table = PageTable

  override val modelClass = classOf[Page]
  override val baseQuery = TableQuery[PageTable]

  registerModel()

  override def like(page: Page): Future[Option[Page]] = find { p =>
    p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase
  }

}
