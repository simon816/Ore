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

  def setColor(channel: Channel, color: Color) = {
    val query = for { model <- this.models if model.id === channel.id.get } yield model.color
    run(query.update(color))
  }

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], channel: Channel): Channel = {
    channel.copy(id = id, createdAt = theTime)
  }

}
