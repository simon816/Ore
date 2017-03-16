package models.user

import java.sql.Timestamp

import db.Named
import db.impl.PendingOrganizationTable
import db.impl.model.OreModel
import db.impl.model.common.Emailable
import db.impl.table.ModelKeys._
import ore.user.UserOwned

/**
  * Represents an attempt at creating an organization. Keeps track of what and
  * what was not created successfully for future attempts.
  *
  * @param id           Unique ID
  * @param createdAt    Instant of creation
  * @param userId       Creator ID
  * @param name         Organization name
  * @param _spongeId    Sponge user ID
  * @param _email       Organization email
  * @param _discourseId Discourse user ID
  */
case class PendingOrganization(override val id: Option[Int] = None,
                               override val createdAt: Option[Timestamp] = None,
                               override val userId: Int,
                               override val name: String,
                               private var _spongeId: Int = -1,
                               private var _email: Option[String] = None,
                               private var _discourseId: Int = -1)
                               extends OreModel(id, createdAt) with UserOwned with Named with Emailable {

  override type T = PendingOrganizationTable
  override type M = PendingOrganization

  /**
    * Returns the ID of the created Sponge user, -1 if not created.
    *
    * @return ID of Sponge user
    */
  def spongeId: Int = this._spongeId

  /**
    * Sets the ID of the created Sponge user.
    *
    * @param spongeId ID of Sponge user
    */
  def spongeId_=(spongeId: Int) = Defined {
    this._spongeId = spongeId
    update(SpongeId)
  }

  /**
    * Returns the ID of the created Discourse user, -1 if not created.
    *
    * @return ID of Discourse user
    */
  def discourseId: Int = this._discourseId

  /**
    * Sets the ID of the created Discourse user.
    *
    * @param discourseId ID of Discourse user
    */
  def discourseId_=(discourseId: Int) = Defined {
    this._discourseId = discourseId
    update(DiscourseId)
  }

  override def email: Option[String] = this._email

  def email_=(email: String) = Defined {
    this._email = Option(email)
    update(Email)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
