package ore.project.member

import models.project.Project

/**
  * Represents a collection of [[Member]]s who work on a project.
  *
  * @param project    Project this team belongs to.
  * @param name       Name of team
  */
case class Team(override val project: Project,
                override val name: String,
                val          members: Set[Member])
                extends Member(project, name)
