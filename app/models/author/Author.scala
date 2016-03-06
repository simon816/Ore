package models.author

/**
  * Represents an author of a Project.
  */
abstract class Author {

  /**
    * Returns the name of this Author
    *
    * @return Name of author
    */
  def name: String

}

/**
  * Author data-store
  */
object Author {

  /**
    * Returns the Author with the specified name.
    *
    * @param name Author name
    * @return Author if exists, None otherwise
    */
  def get(name: String): Option[Author] = Dev.get(name).orElse(Team.get(name))

}