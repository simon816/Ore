package db.query

import db.ChannelTable
import db.OrePostgresDriver.api._
import db.orm.dao.ModelFilter
import db.query.ModelQueries.run
import models.project.Channel
import ore.Colors.Color

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

  /**
    * Sets the [[Color]] of the specified [[Channel]] in the database.
    *
    * @param channel  Channel to set color of
    * @param color    Color to set
    */
  def setColor(channel: Channel, color: Color)
  = run((for {model <- this.baseQuery if model.id === channel.id.get } yield model.color).update(color))

}
