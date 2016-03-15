package util

import scala.util.Try

/**
  * Represents an action pending completion.
  */
trait PendingAction[R] {

  /**
    * Completes the action.
    */
  def complete: Try[R]

  /**
    * Cancels the action.
    */
  def cancel()

}
