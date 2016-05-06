package db.query.impl

import db.PageTable
import db.driver.OrePostgresDriver.api._
import db.query.ModelQueries
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageQueries extends ModelQueries[PageTable, Page](classOf[Page], TableQuery[PageTable]) {

  override def like(page: Page): Future[Option[Page]]
  = find(p => p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase)

}
