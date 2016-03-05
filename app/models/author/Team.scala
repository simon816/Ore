package models.author

/**
  * Represents a collection of developers who work on a project.
  *
  * @param name    Name of team
  * @param members Developers on team
  */
case class Team(name: String, members: List[Dev]) extends Author {

  def this(name: String, owner: Dev) = this(name, List(owner))

}

object Team {

  val teams = Seq(
    new Team("SpongePowered", Dev.get("Spongie").get)
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
