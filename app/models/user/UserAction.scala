package models.user

import java.sql.Timestamp

import com.github.tminglei.slickpg.InetString
import controllers.sugar.Requests.{AuthRequest, OreRequest}
import db.ModelService
import db.impl.UserActionLogTable
import db.impl.model.OreModel
import ore.StatTracker
import ore.user.UserOwned

case class UserAction(override val id: Option[Int] = None,
                      override val createdAt: Option[Timestamp] = None,
                      private val _userId: Int,
                      private val _address: InetString,
                      private val _context: String,
                      private val _context_id: Int,
                      private val _action: String,
                      private val _newState: String,
                      private val _oldState: String
                     )
  extends OreModel(id, createdAt)
    with UserOwned {

  override type T = UserActionLogTable
  override type M = UserAction

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): UserAction = this.copy(createdAt = theTime)

  override def userId: Int = _userId

  def address: InetString = _address

  def action: String = _action

  def oldState = _oldState

  def newState = _newState

}

object UserActions extends Enumeration {
  type UserActions = String

  val PROJECT_VISIBILITY_CHANGE = "VisibilityChange"
  val PROJECT_CREATED = "ProjectCreated"
  val PROJECT_RENAME = "ProjectRename"
  val PROJECT_FLAGGED = "ProjectFlagged"
  val PROJECT_SETTINGS_CHANGED = "ProjectSettingsChanged"
  val PROJECT_MEMBER_REMOVED = "ProjectMemberRemoved"
  val PROJECT_ICON_CHANGED = "ProjectIconChanged"
  val PROJECT_PAGE_EDITED = "ProjectPageEdited"
  val VERSION_DELETED = "VersionDeleted"
  val VERSION_UPLOADED = "VersionUploaded"
  val VERSION_APPROVED = "VersionApproved"
  val VERSION_SET_AS_RECOMMENDED = "VersionSetAsRecommended"
  val VERSION_EDITED_DESCRIPTION = "VersionEditedDescription"
}

object UserActionContexts extends Enumeration {
  type UserActionContexts = String

  val PROJECT = "Project"
  val VERSION = "Version"
}

object UserActionLogger {

  val Logger = play.api.Logger("Operations")

  def log(request: AuthRequest[_], context: String, contextId: Int, action: String, newState: String, oldState: String)(implicit service: ModelService){
    val address = StatTracker.remoteAddress(request.request)
    val user = request.user

    Logger.info(s"${user.name}($address) executed the action '$action'. The state changed from '$oldState' to '$newState'")

    service.insert(UserAction(None, None, user.userId, InetString(address), context, contextId, action, newState, oldState))
  }

}
