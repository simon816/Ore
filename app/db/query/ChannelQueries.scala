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

  /**
    * Returns a filter that filters channels based on the specified name.
    *
    * @param name Name to find
    * @return     Name filter
    */
  def NameFilter(name: String): ModelFilter[ChannelTable, Channel]
  = ModelFilter(_.name.toLowerCase === name.toLowerCase)

}
