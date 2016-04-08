package db.query

import db.ChannelTable
import db.OrePostgresDriver.api._
import Queries._
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

  /**
    * Creates a new Channel.
    *
    * @param channel  Channel to create
    * @return         Newly created channel
    */
  def create(channel: Channel): Future[Channel] = {
    channel.onCreate()
    val channels = q[ChannelTable](classOf[Channel])
    val query = {
      channels returning channels.map(_.id) into {
        case (c, id) =>
          c.copy(id=Some(id))
      } += channel
    }
    DB.run(query)
  }

}
