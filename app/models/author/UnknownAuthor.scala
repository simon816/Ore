package models.author

/**
  * Represents an Author not registered in the system.
  *
  * @param name Name of author
  */
case class UnknownAuthor(override val name: String) extends Author
