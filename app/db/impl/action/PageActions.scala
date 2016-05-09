package db.impl.action

import db.ModelService
import db.action.ModelActions
import db.impl.OrePostgresDriver.api._
import db.impl.PageTable
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageActions(implicit val service: ModelService) extends ModelActions[PageTable, Page](
  classOf[Page], TableQuery[PageTable]) {

  override def like(page: Page): Future[Option[Page]]
  = find(p => p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase)

}
