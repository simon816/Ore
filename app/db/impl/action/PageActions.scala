package db.impl.action

import db.impl.PageTable
import db.impl.pg.OrePostgresDriver.api._
import db.{ModelActions, ModelService}
import models.project.Page

import scala.concurrent.Future

/**
  * Page related queries.
  */
class PageActions(override val service: ModelService)
  extends ModelActions[PageTable, Page](service, classOf[Page], TableQuery[PageTable]) {

  override def like(page: Page): Future[Option[Page]]
  = find(p => p.projectId === page.projectId && p.name.toLowerCase === page.name.toLowerCase)

}
