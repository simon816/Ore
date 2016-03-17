package db

import com.github.tminglei.slickpg._

/**
  * Custom Postgres driver to support array data types.
  */
trait OrePostgresDriver extends ExPostgresDriver with PgArraySupport {

  override val api = OreDriver

  object OreDriver extends API with ArrayImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

}

object OrePostgresDriver extends OrePostgresDriver
