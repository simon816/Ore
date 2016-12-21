package db.impl.model

import java.sql.Timestamp

import db.Model
import db.impl.access.{CompetitionBase, OrganizationBase, ProjectBase, UserBase}
import discourse.OreDiscourseApi
import ore.OreConfig
import security.SpongeAuthApi

/** An Ore Model */
abstract class OreModel(override val id: Option[Int],
                        override val createdAt: Option[Timestamp])
                       (implicit var userBase: UserBase = null,
                        var projectBase: ProjectBase = null,
                        var organizationBase: OrganizationBase = null,
                        var competitionBase: CompetitionBase = null,
                        var config: OreConfig = null,
                        var forums: OreDiscourseApi = null,
                        var auth: SpongeAuthApi = null)
                        extends Model(id, createdAt)
