package models.user

import java.sql.Timestamp

import db.Model
import db.impl.OreModel
import db.meta.{Bind, HasMany}
import models.project.Project
import ore.UserOwned
import ore.permission.role.RoleTypes.RoleType
import ore.permission.scope.{GlobalScope, Scope, ScopeSubject}

import scala.annotation.meta.field

/**
  * Represents an Ore Organization. An organization is like a [[User]] in the
  * sense that it shares many qualities with Users and also has a companion
  * User on the forums. An organization is made up by a group of Users who each
  * have a corresponding rank within the organization.
  *
  * @param id             Unique ID
  * @param createdAt      Date of creation
  * @param name           Organization name
  * @param password       Sponge forums password (encrypted)
  * @param ownerId        The ID of the [[User]] that owns this organization
  * @param _avatarUrl     URL for user avatar
  * @param _tagline       Configured tagline
  * @param _globalRoles   Global roles this organization possesses
  */
@HasMany(Array(classOf[Project]))
case class Organization(override val id: Option[Int] = None,
                        override val createdAt: Option[Timestamp] = None,
                        @(Bind @field) name: String,
                        @(Bind @field) password: String,
                        @(Bind @field) ownerId: Int,
                        @(Bind @field) _avatarUrl: Option[String] = None,
                        @(Bind @field) _tagline: Option[String] = None,
                        @(Bind @field) _globalRoles: List[RoleType] = List())
                        extends OreModel(id, createdAt)
                          with UserLike
                          with UserOwned
                          with ScopeSubject {

  override def username: String = this.name
  override def tagline: Option[String] = this._tagline
  override def globalRoles: Set[RoleType] = this._globalRoles.toSet
  override def joinDate: Option[Timestamp] = this.createdAt
  override def avatarTemplate: Option[String] = this._avatarUrl.map(this.config.forums.getString("baseUrl").get + _)

  override val userId: Int = this.ownerId
  override val scope: Scope = GlobalScope
  override def copyWith(id: Option[Int], theTime: Option[Timestamp]): Model = this.copy(createdAt = theTime)

}
