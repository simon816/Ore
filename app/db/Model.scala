package db

/**
  * Represents a Model in the Database.
  */
trait Model {

  /**
    * Model ID
    *
    * @return ID of model
    */
  def id: Option[Int]

}
