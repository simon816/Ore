package models.user

import java.sql.Timestamp

import db.Named
import db.impl.PendingOrganizationTable
import db.impl.model.OreModel
import db.impl.model.common.Emailable
import db.impl.table.ModelKeys._
import ore.user.UserOwned

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

  def spongeId: Int = this._spongeId

  def spongeId_=(spongeId: Int) = Defined {
    this._spongeId = spongeId
    update(SpongeId)
  }

  override def email: Option[String] = this._email

  def email_=(email: String) = Defined {
    this._email = Option(email)
    update(Email)
  }

  def discourseId: Int = this._discourseId

  def discourseId_=(discourseId: Int) = Defined {
    this._discourseId = discourseId
    update(DiscourseId)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
