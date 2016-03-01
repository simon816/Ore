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

  val url = '/' + id
  var views = 0
  var downloads = 0
  var starred = 0

  override def toString = "%s - %s".format(name, description)

}

object Project {

  var projects = Seq(
    Project("example1", "Example 1", "Description 1", "Author 1"),
    Project("example2", "Example 2", "Description 2", "Author 2"),
    Project("example3", "Example 3", "Description 3", "Author 3"),
    Project("example4", "Example 4", "Description 4", "Author 4"),
    Project("example5", "Example 5", "Description 5", "Author 5")
  )

}