package models

import models.author.{Dev, Author}

/**
  * Represents an Ore package. The specified ID should correspond with the
  * actual plugin id defined in the meta file. (TODO: check meta file in file
  * uploads).
  *
  * @param id Plugin ID
  * @param name Name of plugin
  * @param description Short description of plugin
  * @param owner The owner Author for this project
  * @param authors List of addition authors on this project
  */
case class Project(id: String, name: String, description: String, owner: Author, authors: List[Author]) {

  def this(id: String, name: String, description: String, owner: Author) = this(id, name, description, owner, List(owner))

  val url = "/projects/" + owner.name + '/' + id
  var views = 0
  var downloads = 0
  var starred = 0

  override def toString = "%s - %s".format(name, description)

}

object Project {

  // TODO: Replace with DB
  var projects = Seq(
    new Project("example1", "Example 1", "Description 1", Dev("Author1")),
    new Project("example2", "Example 2", "Description 2", Dev("Author2")),
    new Project("example3", "Example 3", "Description 3", Dev("Author3")),
    new Project("example4", "Example 4", "Description 4", Dev("Author4")),
    new Project("example5", "Example 5", "Description 5", Dev("Author5"))
  )

  def get(owner: String, id: String): Option[Project] = {
    for (project <- projects) {
      if (project.owner.name.equals(owner) && project.id.equals(id)) {
        return Some(project)
      }
    }
    None
  }

}
