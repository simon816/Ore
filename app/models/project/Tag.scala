package models.project

import java.sql.Timestamp

import db.Named
import db.impl.TagTable
import db.impl.model.OreModel
import ore.Colors.Color
import ore.permission.scope.ProjectScope

case class Tag(override val id: Option[Int] = None,
               override val createdAt: Option[Timestamp] = None,
               override val projectId: Int,
               name: String,
               data: String,
               color: Color)
  extends OreModel(id, createdAt)
    with Named
    with ProjectScope {

  override type M = Tag
  override type T = TagTable

  def copyWith(id: Option[Int], theTime: Option[Timestamp]): Tag = this.copy(id = id, createdAt = theTime)
}
