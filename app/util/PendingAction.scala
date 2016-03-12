package util

import scala.util.Try

/**
  * Represents an action pending completion.
  */
trait PendingAction {

  /**
    * Completes the action.
    */
  def complete: Try[Unit]

  /**
    * Cancels the action.
    */
  def cancel()

}
