package models.author

import java.sql.Timestamp

/**
  * Represents a collection of developers who work on a project.
  *
  * @param name    Name of team
  */
case class Team(override val id: Int, override val createdAt: Timestamp, override val name: String) extends Author {

}
