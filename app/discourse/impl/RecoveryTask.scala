package discourse.impl

import akka.actor.Scheduler
import db.impl.OrePostgresDriver.api._
import db.impl.access.ProjectBase

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Task to periodically retry failed Discourse requests.
  */
class RecoveryTask(scheduler: Scheduler,
                   retryRate: FiniteDuration,
                   api: OreDiscourseApi,
                   projects: ProjectBase) extends Runnable {

  var failedTopicAttempts: Set[Int] = Set.empty
  var failedUpdateAttempts: Set[Int] = Set.empty
  var failedDeleteAttempts: Set[Int] = Set.empty

  val Logger = this.api.Logger

  def loadUnhealthyData() = {
    // Load projects without a topic
    this.failedTopicAttempts ++= this.projects.filter(_.topicId === -1).flatMap(_.id)

    // Load projects that need a topic update
    this.failedUpdateAttempts ++= this.projects.filter(_.isTopicDirty).flatMap(_.id)

    this.Logger.info(s"Unhealthy Discourse data loaded:\n$statusString")
  }

  def start() = {
    this.scheduler.schedule(this.retryRate, this.retryRate, this)
    this.Logger.info(s"Discourse recovery task started. First run in ${this.retryRate.toString}.")
  }

  def run() = {
    this.Logger.info(s"Running Discourse recovery task:\n$statusString")

    val topicsToCreate = this.failedTopicAttempts
    this.failedTopicAttempts = Set.empty
    this.projects.in(topicsToCreate).filter(_.topicId.isEmpty).foreach(this.api.createProjectTopic)
  }

  private def statusString: String
  = s"Topics to create: ${this.failedTopicAttempts.size}\n" +
    s"Topics to update: ${this.failedUpdateAttempts.size}\n" +
    s"Topics to delete: ${this.failedDeleteAttempts.size}"

}
