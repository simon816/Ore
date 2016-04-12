package db.query

import java.sql.Timestamp

import db.ChannelTable
import db.OrePostgresDriver.api._
import db.query.Queries.DB.run
import models.project.Channel

import scala.concurrent.Future

/**
  * Channel related queries.
  */
class ChannelQueries extends Queries[ChannelTable, Channel](TableQuery(tag => new ChannelTable(tag))) {

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], channel: Channel): Channel = {
    channel.copy(id = id, createdAt = theTime)
  }

}
