package db.impl.query

import db.ModelService
import db.impl.OrePostgresDriver.api._
import db.impl.PageTable
import db.query.ModelQueries
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageQueries(implicit val service: ModelService) extends ModelQueries[PageTable, Page](
  classOf[Page], TableQuery[PageTable]) {

  override def like(page: Page): Future[Option[Page]]
  = find(p => p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase)

}
