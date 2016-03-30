package models.author

trait Author {

  /**
    * Returns the name of this Author.
    *
    * @return Name of author
    */
  def name: String

  /**
    * Returns true if this Author is registered with Ore.
    *
    * @return True if registered, false otherwise
    */
  def isRegistered: Boolean = !isInstanceOf[UnknownAuthor]

}


