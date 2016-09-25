package forums

import akka.actor.{Cancellable, Scheduler}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
  * Represents an background task that retries failed forum requests at a
  * given interval.
  *
  * @param scheduler  Scheduler instance
  * @param delay      Delay between retries
  */
class DiscourseSync(scheduler: Scheduler, delay: FiniteDuration) extends Runnable {

  private var tasks: List[() => Unit] = List.empty
  private var currentTask: Cancellable = _

  /**
    * Schedules a function to be executed on the next pass.
    *
    * @param f Function to run
    */
  def scheduleRetry(f: () => Unit) = {
    this.tasks :+= f
    if (this.currentTask == null)
      this.currentTask = this.scheduler.scheduleOnce(delay, this)
  }

  def run() = {
    // Reset this instance and run the queued tasks
    val toRun = this.tasks
    this.tasks = List.empty
    this.currentTask = null
    for (task <- toRun)
      task()

    // Check if any of the just run tasks failed
    if (this.tasks.nonEmpty)
      this.scheduler.scheduleOnce(delay, this)
  }

}
