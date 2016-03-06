package models.author

import com.google.common.base.{MoreObjects, Objects}

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

  /**
    * Returns true if this Author is registered with Ore.
    *
    * @return True if registered, false otherwise
    */
  def isRegistered = !isInstanceOf[Author.Unknown]

  override def toString = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode = Objects.hashCode(this.name)

  override def equals(o: Any) = o.isInstanceOf[Author] && o.asInstanceOf[Author].name.equals(this.name)

}

/**
  * Author data-store
  */
object Author {

  /**
    * Represents an Author not registered in the system.
    *
    * @param name Name of author
    */
  case class Unknown(override val name: String) extends Author {

  }

  /**
    * Returns the Author with the specified name.
    *
    * @param name Author name
    * @return Author if exists, Unknown author otherwise
    */
  def get(name: String): Author = Dev.get(name).getOrElse(Team.get(name).getOrElse(Unknown(name)))

}