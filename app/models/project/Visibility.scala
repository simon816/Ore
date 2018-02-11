package models.project

import db.impl.OrePostgresDriver
import db.table.MappedType
import ore.permission.{Permission, ReviewProjects}
import slick.jdbc.JdbcType


object VisibilityTypes extends Enumeration {
  val Public          = Visibility(1, "public"        , ReviewProjects      , "")
  val New             = Visibility(2, "new"           , ReviewProjects      , "project-new")
  val NeedsChanges    = Visibility(3, "needsChanges"  , ReviewProjects      , "striped project-needsChanges")
  val NeedsApproval   = Visibility(4, "needsApproval" , ReviewProjects      , "striped project-needsChanges")
  val SoftDelete      = Visibility(5, "softDelete"    , ReviewProjects      , "striped project-hidden")

  def withId(id: Int): Visibility = {
    this.apply(id).asInstanceOf[Visibility]
  }

  case class Visibility(override val id: Int, nameKey: String, permission: Permission, cssClass: String) extends super.Val(id) with MappedType[Visibility] {
    implicit val mapper: JdbcType[Visibility] = OrePostgresDriver.api.visibiltyTypeMapper
  }

  implicit def convert(value: Value): Visibility = value.asInstanceOf[Visibility]
}
