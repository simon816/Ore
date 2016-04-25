package db

import com.github.tminglei.slickpg._
import ore.Colors
import ore.Colors.Color
import ore.permission.role.RoleTypes
import ore.permission.role.RoleTypes.RoleType
import ore.project.Categories.Category
import ore.project.FlagReasons.FlagReason
import ore.project.{Categories, FlagReasons}

/**
  * Custom Postgres driver to support array data and custom type mappings.
  */
trait OrePostgresDriver extends ExPostgresDriver with PgArraySupport {

  override val api = OreDriver

  object OreDriver extends API with ArrayImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val colorTypeMapper = MappedJdbcType.base[Color, Int](_.id, Colors.apply)
    implicit val roleTypeTypeMapper = MappedJdbcType.base[RoleType, Int](_.roleId, RoleTypes.withId)
    implicit val categoryTypeMapper = MappedJdbcType.base[Category, Int](_.id, Categories.apply)
    implicit val flagReasonTypeMapper = MappedJdbcType.base[FlagReason, Int](_.id, FlagReasons.apply)
  }

}

object OrePostgresDriver extends OrePostgresDriver
