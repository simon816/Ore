package db.impl

import java.sql.Timestamp

import db.Model
import db.action.ModelActions
import forums.DiscourseApi
import ore.project.util.ProjectFileManager
import util.OreConfig

/** An Ore Model */
abstract class OreModel[A <: ModelActions[_, _]](override val id: Option[Int],
                        override val createdAt: Option[Timestamp],
                        override val driver: OrePostgresDriver = OrePostgresDriver)
                        extends Model[A](id, createdAt, driver) {
  implicit var config: OreConfig = null
}
