package db.orm.model

import java.sql.Timestamp
import java.text.SimpleDateFormat

import play.api.Play.{configuration => config, current}

/**
  * Represents a Model in the Database.
  */
abstract class Model {

  /**
    * The format used for displaying dates for models.
    */
  val DateFormat = new SimpleDateFormat(config.getString("ore.date-format").get)

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

  /**
    * Returns a presentable date string of this models's creation date.
    *
    * @return Creation date string
    */
  def prettyDate: String = DateFormat.format(this.createdAt.get)

  /**
    * Returns true if this Project is defined in the database.
    *
    * @return True if defined in database
    */
  def isDefined: Boolean = this.id.isDefined

  protected def assertDefined[A](f: => A): A = {
    if (isDefined) {
      f
    } else {
      throw new IllegalStateException("model must exist")
    }
  }


}
