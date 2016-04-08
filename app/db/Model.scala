package db

import java.sql.Timestamp

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

  /**
    * The Timestamp instant that this Model was created.
    *
    * @return Instant of creation
    */
  def createdAt: Option[Timestamp]

}
