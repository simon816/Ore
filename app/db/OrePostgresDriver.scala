package db

import com.github.tminglei.slickpg._
import ore.Colors
import ore.Colors.Color
import ore.permission.role.{Role, RoleTypes}
import ore.permission.role.RoleTypes.RoleType

/**
  * Custom Postgres driver to support array data types.
  */
trait OrePostgresDriver extends ExPostgresDriver with PgArraySupport {

  override val api = OreDriver

  object OreDriver extends API with ArrayImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
    implicit val colorTypeMapper = MappedJdbcType.base[Color, Int](_.id, Colors.apply)
    implicit val roleTypeTypeMapper = MappedJdbcType.base[RoleType, Int](_.id, RoleTypes.apply)
  }

}

object OrePostgresDriver extends OrePostgresDriver
