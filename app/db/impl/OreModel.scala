package db.impl

import java.sql.Timestamp

import db.Model
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import forums.DiscourseApi
import ore.OreConfig

/** An Ore Model */
abstract class OreModel(override val id: Option[Int],
                        override val createdAt: Option[Timestamp])
                       (implicit var userBase: UserBase = null,
                        var projectBase: ProjectBase = null,
                        var organizationBase: OrganizationBase = null,
                        var config: OreConfig = null,
                        var forums: DiscourseApi = null)
                        extends Model(id, createdAt)
