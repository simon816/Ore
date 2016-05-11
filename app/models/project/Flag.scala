package models.project

import java.sql.Timestamp

import db.ModelService
import db.action.{ModelActions, ModelAccess}
import db.impl.ModelKeys._
import db.impl.OrePostgresDriver.api._
import db.impl.{FlagTable, OreModel}
import db.meta.{Actor, Bind}
import ore.UserOwned
import ore.permission.scope.ProjectScope
import ore.project.FlagReasons.FlagReason

import scala.annotation.meta.field

/**
  * Represents a flag on a Project that requires staff attention.
  *
  * @param id           Unique ID
  * @param createdAt    Timestamp instant of creation
  * @param projectId    Project ID
  * @param userId       Reporter ID
  * @param reason       Reason for flag
  * @param _isResolved  True if has been reviewed and resolved by staff member
  */
@Actor(classOf[ModelActions[FlagTable, Flag]])
case class Flag(override val id: Option[Int],
                override val createdAt: Option[Timestamp],
                override val projectId: Int,
                override val userId: Int,
                reason: FlagReason,
                @(Bind @field) private var _isResolved: Boolean = false)
                extends OreModel(id, createdAt)
                  with UserOwned
                  with ProjectScope { self =>

  def this(projectId: Int, userId: Int, reason: FlagReason) = {
    this(id=None, createdAt=None, projectId=projectId, userId=userId, reason=reason)
  }

  /**
    * Returns true if this Flag has been reviewed and marked as resolved by a
    * staff member.
    *
    * @return True if resolved
    */
  def isResolved: Boolean = this._isResolved

  /**
    * Sets whether this Flag has been marked as resolved.
    *
    * @param resolved True if resolved
    */
  def setResolved(resolved: Boolean) = Defined {
    this._isResolved = resolved
    update(IsResolved)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Flag = this.copy(id = id, createdAt = theTime)

}

object Flag {

  /**
    * Returns all Flags that are unresolved.
    *
    * @return All unresolved flags
    */
  def unresolved(implicit service: ModelService): Seq[Flag]
  = service.access[FlagTable, Flag](classOf[Flag]).filterNot(_.isResolved)

}
