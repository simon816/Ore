package db.query

import java.sql.Timestamp

import db.ChannelTable
import db.OrePostgresDriver.api._
import db.query.Queries._
import models.project.Channel

import scala.concurrent.Future

/**
  * Channel related queries.
  */
object ChannelQueries extends ModelQueries[ChannelTable, Channel] {

  /**
    * Returns all Channels in the specified Project.
    *
    * @param projectId  Project to get channels of
    * @return           Channels in project
    */
  def in(projectId: Int): Future[Seq[Channel]] = {
    filter[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId)
  }

  /**
    * Returns the Channel with the specified name in the specified project.
    *
    * @param projectId  Project ID
    * @param name       Channel name
    * @return           Channel with name if any, None otherwise
    */
  def withName(projectId: Int, name: String): Future[Option[Channel]] = {
    find[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.name.toLowerCase === name.toLowerCase)
  }

  /**
    * Returns the Channel with the specified color in the specified project.
    *
    * @param projectId  Project ID
    * @param colorId    Channel color
    * @return           Channel with color if any, None otherwise
    */
  def withColor(projectId: Int, colorId: Int): Future[Option[Channel]] = {
    find[ChannelTable, Channel](classOf[Channel], c => c.projectId === projectId && c.colorId === colorId)
  }

  /**
    * Returns the Channel with the specified ID.
    *
    * @param id   Channel ID
    * @return     Channel with id if any, None otherwise
    */
  def withId(id: Int): Future[Option[Channel]] = find[ChannelTable, Channel](classOf[Channel], c => c.id === id)

  override def copyInto(id: Option[Int], theTime: Option[Timestamp], channel: Channel): Channel = {
    channel.copy(id = id, createdAt = theTime)
  }

}
