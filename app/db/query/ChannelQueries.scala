package db.query

import db.OrePostgresDriver.api._
import db.orm.dao.ModelSet
import db.query.Queries.{ModelFilter, run}
import db.{ChannelTable, VersionTable}
import models.project.{Channel, Version}
import ore.Colors.Color

/**
  * Channel related queries.
  */
class ChannelQueries extends Queries {

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
    * Returns the [[Version]]'s for the specified Channel.
    *
    * @param channel  Channel to get versions for
    * @return         Versions in channel
    */
  def getVersions(channel: Channel): ModelSet[ChannelTable, Channel, VersionTable, Version]
  = Queries.getModelSet[ChannelTable, Channel, VersionTable, Version](classOf[Version], _.channelId, channel)

  /**
    * Sets the [[Color]] of the specified [[Channel]] in the database.
    *
    * @param channel  Channel to set color of
    * @param color    Color to set
    */
  def setColor(channel: Channel, color: Color)
  = run((for {model <- this.baseQuery if model.id === channel.id.get } yield model.color).update(color))

}
