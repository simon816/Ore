package models.author

/**
  * Represents a single developer on a project.
  *
  * @param name Name of developer
  */
case class Dev(name: String) extends Author {

}

/**
  * Dev data-store
  */
object Dev {

  // TODO: Replace with DB
  val devs = Seq(
    Dev("Spongie"),
    Dev("Author1"),
    Dev("Author2"),
    Dev("Author3"),
    Dev("Author4"),
    Dev("Author5")
  )

  /**
    * Returns the Dev with the specified name.
    *
    * @param name Dev name
    * @return Dev if exists, None otherwise
    */
  def get(name: String): Option[Dev] = {
    for (dev <- devs) {
      if (dev.name.equals(name)) {
        return Some(dev)
      }
    }
    None
  }

}
