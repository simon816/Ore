package db.impl

import java.sql.Timestamp

import db.Model
import util.OreConfig

/** An Ore Model */
abstract class OreModel(override val id: Option[Int],
                        override val createdAt: Option[Timestamp],
                        override val driver: OrePostgresDriver = OrePostgresDriver)
                        extends Model(id, createdAt, driver) {
  implicit var config: OreConfig = null
}
