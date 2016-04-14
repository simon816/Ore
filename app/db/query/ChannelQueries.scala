package db.query

import java.sql.Timestamp

import db.ChannelTable
import db.OrePostgresDriver.api._
import db.query.Queries.DB.run
import models.project.Channel
import ore.Colors.Color

/**
  * Channel related queries.
  */
class ChannelQueries extends Queries[ChannelTable, Channel](TableQuery(tag => new ChannelTable(tag))) {

  def setColor(channelId: Int, color: Color) = {
    val query = for { channel <- this.models if channel.id === channelId } yield channel.color
    run(query.update(color))
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], channel: Channel): Channel = {
    channel.copy(id = id, createdAt = theTime)
  }

}
