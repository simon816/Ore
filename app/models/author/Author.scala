package models.author

import com.google.common.base.{MoreObjects, Objects}
import models.project.Project

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
    * Returns the Project with the specified name that this Author owns.
    *
    * @param name Name of project
    * @return Owned project, if any, None otherwise
    */
  def getProject(name: String): Option[Project] = Project.get(this, name)

  /**
    * Returns all Projects owned by this Author.
    *
    * @return All projects owned by Author
    */
  def getProjects: Set[Project] = Project.getAll(this)

  /**
    * Returns true if this Author is registered with Ore.
    *
    * @return True if registered, false otherwise
    */
  def isRegistered: Boolean = !isInstanceOf[Author.Unknown]

  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode: Int = Objects.hashCode(this.name)

  override def equals(o: Any): Boolean = o.isInstanceOf[Author] && o.asInstanceOf[Author].name.equals(this.name)

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