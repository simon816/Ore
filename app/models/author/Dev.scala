package models.author

import java.sql.Timestamp

/**
  * Represents a single developer on a project.
  *
  * @param id         Unique identifier
  * @param createdAt  Instant of creation
  * @param name       Name of developer
  */
case class Dev(override val id: Option[Int], override val createdAt: Option[Timestamp],
               override val name: String) extends Author
