package models.project

import java.sql.Timestamp

import db.FlagTable
import db.dao.ModelSet
import db.driver.OrePostgresDriver.api._
import db.model.Model
import db.model.ModelKeys._
import db.model.annotation.{Bind, BindingsGenerator}
import ore.UserOwner
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
case class Flag(override val  id: Option[Int],
                override val  createdAt: Option[Timestamp],
                override val  projectId: Int,
                override val  userId: Int,
                              reason: FlagReason,
                @(Bind @field) private var _isResolved: Boolean = false)
                extends Model(id, createdAt) with UserOwner with ProjectScope { self =>

  override type M <: Flag { type M = self.M }

  BindingsGenerator.generateFor(this)

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

object Flag extends ModelSet[FlagTable, Flag](classOf[Flag]) {

  /**
    * Returns all Flags that are unresolved.
    *
    * @return All unresolved flags
    */
  def unresolved: Seq[Flag] = this.filter(!_.isResolved)

}
