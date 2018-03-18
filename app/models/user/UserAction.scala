package models.user

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString
import db.ModelService
import db.impl.UserActionLogTable
import db.impl.model.OreModel
import ore.StatTracker
import ore.permission.Permission
import ore.user.UserOwned
import play.api.mvc.RequestHeader

import scala.util.{Failure, Success}

case class UserAction(override val id: Option[Int] = None,
                      override val createdAt: Option[Timestamp] = None,
                      private val _userId: Int,
                      private val _address: InetString,
                      private val _action: String
                     )
  extends OreModel(id, createdAt)
    with UserOwned {


  override type T = UserActionLogTable
  override type M = UserAction

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): UserAction = this.copy(createdAt = theTime)

  override def userId: Int = _userId

  def address: InetString = _address

  def action: String = _action

}

object UserActionLogger {

  val Logger = play.api.Logger("Permissions")

  def log(user: User, action: String)(implicit service: ModelService, request: RequestHeader): Unit = {
    val address = StatTracker.remoteAddress(request)
    Logger.info(s"${user.name} did '$action'. ($address)")
    service.insert(UserAction(None, None, user.userId, InetString(address), action))
  }

}
