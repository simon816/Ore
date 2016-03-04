package models

/**
  * Represents an Ore package. The specified ID should correspond with the
  * actual plugin id defined in the meta file. (TODO: check meta file in file
  * uploads).
  *
  * @param id Plugin ID
  * @param name Name of plugin
  * @param description Short description of plugin
  */
case class Project(id: String, name: String, description: String, author: String) {

  val url = "/projects/" + author + '/' + id
  var views = 0
  var downloads = 0
  var starred = 0

  override def toString = "%s - %s".format(name, description)

}

object Project {

  // TODO: Replace with DB
  var projects = Seq(
    Project("example1", "Example 1", "Description 1", "Author1"),
    Project("example2", "Example 2", "Description 2", "Author2"),
    Project("example3", "Example 3", "Description 3", "Author3"),
    Project("example4", "Example 4", "Description 4", "Author4"),
    Project("example5", "Example 5", "Description 5", "Author5")
  )

  def get(author: String, id: String): Option[Project] = {
    for (project <- projects) {
      if (project.author.equals(author) && project.id.equals(id)) {
        return Some(project)
      }
    }
    None
  }

}
