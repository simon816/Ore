package db.query

import db.ChannelTable
import db.dao.ModelFilter
import db.driver.OrePostgresDriver.api._
import models.project.Channel

/**
  * Channel related queries.
  */
class ChannelQueries extends ModelQueries {

  override type Row = Channel
  override type Table = ChannelTable

  override val modelClass = classOf[Channel]
  override val baseQuery = TableQuery[ChannelTable]

  registerModel()

}
