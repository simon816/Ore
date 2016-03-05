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

object Author {

  def get(name: String): Option[Author] = Dev.get(name).orElse(Team.get(name))

}