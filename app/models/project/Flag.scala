package models.project

import java.sql.Timestamp

import db.OrePostgresDriver.api._
import db.orm.dao.ModelDAO
import db.orm.model.{UserOwner, Model}
import db.orm.model.ModelKeys._
import db.query.Queries
import db.query.Queries.now
import ore.permission.scope.ProjectScope
import ore.project.FlagReasons.FlagReason

case class Flag(override val  id: Option[Int],
                override val  createdAt: Option[Timestamp],
                override val  projectId: Int,
                override val  userId: Int,
                val           reason: FlagReason,
                private var   _isResolved: Boolean = false)
                extends       Model
                with          UserOwner
                with          ProjectScope { self =>

  def this(projectId: Int, userId: Int, reason: FlagReason) = {
    this(id=None, createdAt=None, projectId=projectId, userId=userId, reason=reason)
  }

  def isResolved: Boolean = this._isResolved

  def setResolved(resolved: Boolean) = assertDefined {
    this._isResolved = resolved
    update(IsResolved)
  }

  // Table bindings

  override type M <: Flag { type M = self.M }

  bind[Boolean](IsResolved, _._isResolved, isResolved => {
    Seq(Queries.Projects.Flags.setBoolean(this, _.isResolved, isResolved))
  })

}

object Flag extends ModelDAO[Flag] {

  def all: Seq[Flag] = now(Queries.Projects.Flags.collect()).get

  def unresolved: Seq[Flag] = now(Queries.Projects.Flags.collect(filter = !_.isResolved)).get

  override def withId(id: Int): Option[Flag] = now(Queries.Projects.Flags.get(id)).get

}
