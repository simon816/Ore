package models.project

import java.sql.Timestamp

import db.Named
import db.impl.TagTable
import db.impl.model.OreModel
import db.impl.table.ModelKeys._
import ore.Colors.Color

case class Tag(override val id: Option[Int] = None,
               private var _versionIds: List[Int],
               name: String,
               data: String,
               color: Color)
  extends OreModel(id, None)
    with Named {

  override type M = Tag
  override type T = TagTable

  def versionIds: List[Int] = this._versionIds

  def addVersionId(versionId: Int) = {
    this._versionIds = this._versionIds :+ versionId
    if (isDefined) {
      update(TagVersionIds)
    }
  }

  def copyWith(id: Option[Int], theTime: Option[Timestamp]): Tag = this.copy(id = id)
}
