package models.user

import java.sql.Timestamp

import db.Model
import db.impl.ProjectTable
import models.project.Project
import ore.permission.role.RoleTypes.RoleType
import util.OreConfig

/**
  * Represents something that has user-like qualities like a [[User]] or an
  * [[Organization]].
  */
trait UserLike extends Model {

  /**
    * Returns an instance of an [[OreConfig]].
    *
    * @return Ore config
    */
  def config: OreConfig

  /**
    * The "username" of this UserLike object.
    *
    * @return Username
    */
  def username: String

  /**
    * Returns a template string for constructing an avatar URL.
    *
    * @return Avatar template
    */
  def avatarTemplate: Option[String]

  /**
    * Returns a URL to this user's avatar for the specified size
    *
    * @param size avatar size
    * @return     avatar url
    */
  def avatarUrl(size: Int = 100): String = this.avatarTemplate.map { s =>
    s.replace("{size}", size.toString)
  }.getOrElse("")

  /**
    * Returns this user's tagline.
    *
    * @return User tagline
    */
  def tagline: Option[String]

  /**
    * Returns this user's global [[RoleType]]s.
    *
    * @return Global RoleTypes
    */
  def globalRoles: Set[RoleType]

  /**
    * Returns all [[Project]]s owned by this user.
    *
    * @return Projects owned by user
    */
  def projects = this.oneToMany[ProjectTable, Project](classOf[Project])

  /**
    * Returns the date this User joined any Sponge services.
    *
    * @return Join date
    */
  def joinDate: Option[Timestamp]

  /**
    * Returns the date of creation on Ore.
    *
    * @return Creation date
    */
  def createdAt: Option[Timestamp]

}
