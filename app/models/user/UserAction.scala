package models.user

import java.sql.Timestamp

import db.impl.UserActionLogTable
import db.impl.model.OreModel
import ore.user.UserOwned

case class UserAction(override val id: Option[Int] = None,
                      override val createdAt: Option[Timestamp] = None,
                      private val _userId: Int,
                      private val _action: String
                     )
  extends OreModel(id, createdAt)
    with UserOwned {


  override type T = UserActionLogTable
  override type M = UserAction

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): UserAction = this.copy(createdAt = theTime)

  override def userId: Int = _userId

  def action: String = _action

}
