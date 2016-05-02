package db.query

import db.FlagTable
import db.driver.OrePostgresDriver.api._
import models.project.Flag

/**
  * Flag related queries.
  */
class FlagQueries extends ModelQueries {
  override type Row = Flag
  override type Table = FlagTable
  override val modelClass = classOf[Flag]
  override val baseQuery = TableQuery[FlagTable]
  registerModel()
}
