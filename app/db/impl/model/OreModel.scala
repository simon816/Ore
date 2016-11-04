package db.impl.model

import java.sql.Timestamp

import db.Model
import db.impl.access.{OrganizationBase, ProjectBase, UserBase}
import discourse.OreDiscourseApi
import ore.OreConfig

/** An Ore Model */
abstract class OreModel(override val id: Option[Int],
                        override val createdAt: Option[Timestamp])
                       (implicit var userBase: UserBase = null,
                        var projectBase: ProjectBase = null,
                        var organizationBase: OrganizationBase = null,
                        var config: OreConfig = null,
                        var forums: OreDiscourseApi = null)
                        extends Model(id, createdAt)
