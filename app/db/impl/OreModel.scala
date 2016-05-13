package db.impl

import java.sql.Timestamp

import db.Model
import db.impl.service.{ProjectBase, UserBase}
import forums.DiscourseApi
import util.OreConfig

/** An Ore Model */
abstract class OreModel(override val id: Option[Int],
                        override val createdAt: Option[Timestamp],
                        override val driver: OrePostgresDriver = OrePostgresDriver)
                       (implicit var userBase: UserBase = null,
                        var projectBase: ProjectBase = null,
                        var config: OreConfig = null,
                        var forums: DiscourseApi = null)
                        extends Model(id, createdAt, driver)
