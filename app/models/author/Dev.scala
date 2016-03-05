package models.author

case class Dev(name: String) extends Author {

  override def url: String = "/projects/" + name

}

object Dev {

  val devs = Seq(
    Dev("windy1"),
    Dev("Zidane"),
    Dev("gabizou")
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
