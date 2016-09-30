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

  /**
    * Loads incomplete or missing data regarding Discourse information from
    * the database and caches it to be rectified the next time this task is
    * run.
    */
  def loadUnhealthyData() = {
    // Load projects without a topic
    this.failedTopicAttempts ++= this.projects.filter(_.topicId === -1).flatMap(_.id)

    // Load projects that need a topic update
    this.failedUpdateAttempts ++= this.projects.filter(_.isTopicDirty).flatMap(_.id)

    // TODO: Failed delete attempts

    this.Logger.info(s"Unhealthy Discourse data loaded:\n$statusString")
  }

  /**
    * Starts the recovery task to be run at the specified interval.
    */
  def start() = {
    this.scheduler.schedule(this.retryRate, this.retryRate, this)
    this.Logger.info(s"Discourse recovery task started. First run in ${this.retryRate.toString}.")
  }

  override def run() = {
    this.Logger.info(s"Running Discourse recovery task:\n$statusString")

    val topicsToCreate = this.failedTopicAttempts
    this.failedTopicAttempts = Set.empty
    this.projects.in(topicsToCreate).filter(_.topicId == -1).foreach(this.api.createProjectTopic)

    val topicsToUpdate = this.failedUpdateAttempts
    this.failedUpdateAttempts = Set.empty
    this.projects.in(topicsToUpdate).filter(_.isTopicDirty).foreach(this.api.updateProjectTopic)

    val topicsToDelete = this.failedDeleteAttempts
    this.failedDeleteAttempts = Set.empty
    this.projects.in(topicsToDelete).filterNot(_.topicId == -1).foreach(this.api.deleteProjectTopic)
  }

  private def statusString: String
  = s"Topics to create: ${this.failedTopicAttempts.size}\n" +
    s"Topics to update: ${this.failedUpdateAttempts.size}\n" +
    s"Topics to delete: ${this.failedDeleteAttempts.size}"

}
