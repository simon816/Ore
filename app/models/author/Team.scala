package models.author

case class Team(name: String, members: List[Author]) extends Author {

  def this(name: String, owner: Dev) = this(name, List(owner))

}

object Team {

  val teams = Seq(
    new Team("SpongePowered", Dev.get("windy1").get)
  )

  def get(name: String): Option[Team] = {
    for (team <- teams) {
      if (team.name.equals(name)) {
        return Some(team)
      }
    }
    None
  }

}
