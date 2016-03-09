package models.author

import java.sql.Timestamp

/**
  * Represents a single developer on a project.
  *
  * @param id         Unique identifier
  * @param createdAt  Instant of creation
  * @param name       Name of developer
  */
case class Dev(override val id: Int, override val createdAt: Timestamp, override val name: String) extends Author {

}
