package models.user

import java.sql.Timestamp

import db.Model
import db.impl.OreModel
import db.meta.Bind
import ore.UserOwned
import ore.permission.scope.{GlobalScope, Scope, ScopeSubject}

import scala.annotation.meta.field

case class Organization(override val id: Option[Int] = None,
                        override val createdAt: Option[Timestamp] = None,
                        @(Bind @field) name: String,
                        @(Bind @field) password: String,
                        @(Bind @field) ownerId: Int)
                        extends OreModel(id, createdAt)
                          with UserOwned
                          with ScopeSubject {

  override val userId: Int = this.ownerId
  override val scope: Scope = GlobalScope
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(createdAt = theTime)

}
