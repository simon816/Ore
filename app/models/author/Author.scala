package models.author

/**
  * Represents an author of a Project. Authors can be either a Team or Dev.
  * Every project has a single Author designated as the "owner" and then a list
  * of additional Authors on the project. The list of additional authors
  * should include the owner. Team and Dev names must be unique to one another.
  * That is, a Dev cannot have the same name as another Dev nor may it have the
  * same name as a Team and vice-versa.
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