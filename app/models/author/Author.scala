package models.author

abstract class Author {

  def name: String

}

object Author {

  def get(name: String): Option[Author] = Dev.get(name).orElse(Team.get(name))

}