package pg

import com.github.tminglei.slickpg._

trait OrePostgresDriver extends ExPostgresDriver with PgArraySupport {

  override val api = OreDriver

  object OreDriver extends API with ArrayImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

}

object OrePostgresDriver extends OrePostgresDriver
