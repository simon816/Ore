package models.author

case class Team(name: String, members: List[Author]) extends Author {

  override def url: String = "/projects/teams/" + name

}
