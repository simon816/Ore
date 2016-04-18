package ore.project.member

import models.project.Project

/**
  * Represents a collection of members who work on a project.
  *
  * @param project    Project this team belongs to.
  * @param name       Name of team
  */
case class Team(override val project: Project, override val name: String, members: Set[Member])
                extends Member(project, name)
