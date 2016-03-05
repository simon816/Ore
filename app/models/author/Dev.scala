package models.author

/**
 * Represents a single developer on a project.
 *
 * @param name Name of developer
 */
case class Dev(name: String) extends Author {

}

object Dev {

  val devs = Seq(
    Dev("windy1"),
    Dev("Zidane"),
    Dev("gabizou"),
    Dev("Author1"),
    Dev("Author2"),
    Dev("Author3"),
    Dev("Author4"),
    Dev("Author5")
  )

  def get(name: String): Option[Dev] = {
    for (dev <- devs) {
      if (dev.name.equals(name)) {
        return Some(dev)
      }
    }
    None
  }

}
