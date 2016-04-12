package models.project.author

import com.google.common.base.MoreObjects
import models.project.Project

/**
  * Represents an author of a Project. Authors can be either a Team or Dev.
  * Every project has a single Author designated as the "owner" and then a list
  * of additional Authors on the project. Team and Dev names must be unique to
  * one another. That is, a Dev cannot have the same name as another Dev nor
  * may it have the same name as a Team and vice-versa.
  */
trait Author {

  /**
    * Returns the name of this Author.
    *
    * @return Name of author
    */
  def name: String

  /**
    * Returns the Project with the specified name that this Author owns.
    *
    * @param name   Name of project
    * @return       Owned project, if any, None otherwise
    */
  def project(name: String): Option[Project] = Project.withName(this.name, name)

  /**
    * Returns all Projects owned by this Author.
    *
    * @return All projects owned by Author
    */
  def projects: Seq[Project] = Project.by(this.name)

  override def toString: String = MoreObjects.toStringHelper(this).add("name", this.name).toString

  override def hashCode: Int = this.name.hashCode

  override def equals(o: Any): Boolean = {
    o.isInstanceOf[Author] && o.asInstanceOf[Author].name.equals(this.name)
  }

}
