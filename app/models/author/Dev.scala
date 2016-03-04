package models.author

case class Dev(name: String) extends Author {

  override def url: String = "/projects/" + name

}
