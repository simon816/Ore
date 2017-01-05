package models.user

import java.sql.Timestamp

import db.impl.SignOnTable
import db.impl.model.OreModel
import db.impl.table.ModelKeys._

case class SignOn(override val id: Option[Int] = None,
                  override val createdAt: Option[Timestamp] = None,
                  nonce: String,
                  var _isCompleted: Boolean = false) extends OreModel(id, createdAt) {

  override type M = SignOn
  override type T = SignOnTable

  def isCompleted: Boolean = this._isCompleted

  def setCompleted(completed: Boolean = true) = Defined {
    this._isCompleted = completed
    update(IsCompleted)
  }

  override def copyWith(id: Option[Int], theTime: Option[Timestamp]) = this.copy(id = id, createdAt = theTime)

}
