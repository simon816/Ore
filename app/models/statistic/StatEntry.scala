package models.statistic

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString
import com.google.common.base.Preconditions._
import db.Model
import db.impl.ModelKeys._
import db.impl.OreModel
import db.impl.action.StatActions
import db.meta.Actions
import models.user.User

/**
  * Represents a statistic entry in a StatTable.
  *
  * @param id         Unique ID of entry
  * @param createdAt  Timestamp instant of creation
  * @param modelId    ID of model the stat is on
  * @param address    Client address
  * @param cookie     Browser cookie
  * @param userId     User ID
  */
@Actions(classOf[StatActions[_, _]])
abstract class StatEntry[Subject <: Model](override val id: Option[Int] = None,
                                           override val createdAt: Option[Timestamp] = None,
                                           val modelId: Int,
                                           val address: InetString,
                                           val cookie: String,
                                           private var userId: Option[Int] = None)
                                           extends OreModel(id, createdAt) {

  checkNotNull(address, "client address cannot be null", "")
  checkNotNull(cookie, "browser cookie cannot be null", "")

  /**
    * Returns the User associated with this entry, if any.
    *
    * @return User of entry
    */
  def user: Option[User] = this.userId.flatMap(this.userBase.get)

  /**
    * Sets the User associated with this entry, if any.
    *
    * @param user User of entry
    */
  def user_=(user: User) = {
    this.userId = user.id
    update(UserId)
  }

}
