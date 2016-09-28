package discourse.impl

import java.nio.file.Path

import akka.actor.Scheduler
import com.google.common.base.Preconditions.{checkArgument, checkNotNull}
import db.impl.access.ProjectBase
import discourse.{DiscourseApi, DiscourseSSO}
import models.project.Project
import models.user.User
import play.api.Logger
import util.StringUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

trait OreDiscourseApi extends DiscourseApi with DiscourseSSO {

  val Logger: Logger = play.api.Logger("Discourse")
  var projects: ProjectBase = _

  protected val categorySlug: String
  protected val topicTemplatePath: Path
  protected val retryRate: FiniteDuration
  protected val scheduler: Scheduler

  private var recovery: RecoveryTask = _

  def start() = {
    checkNotNull(this.projects, "projects are null", "")
    this.recovery = new RecoveryTask(this.scheduler, this.retryRate, this, this.projects)
    this.recovery.start()
    Logger.info("Discourse API initialized.")
  }

  def createProjectTopic(project: Project) = {
    checkArgument(project.id.isDefined, "undefined project", "")
    val content = buildProjectTopic(project)
    val title = buildProjectTitle(project)
    createTopic(
      poster = project.ownerName,
      title = title,
      content = content,
      categorySlug = this.categorySlug
    ).map(_.left.toOption.getOrElse(List.empty)).andThen {
      case Success(errors) => if (errors.nonEmpty) {
        // Request went through but Discourse responded with errors
        this.Logger.warn(
          "Request to create project topic was successful but Discourse responded with errors:\n" +
            s"Project: ${project.url}\n" +
            s"Title: $title\n" +
            s"Content: $content\n" +
            s"Errors: ${errors.toString}")
      }
      case Failure(e) =>
        // Discourse never received our request!
        this.recovery.failedTopicAttempts += project.id.get
    }
  }

  def updateProjectTopic(project: Project): Future[List[String]] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    checkArgument(project.topicId.isDefined, "undefined topic id", "")
    val topicId = project.topicId.get
    val title = buildProjectTitle(project)
    updateTopic(
      username = project.ownerName,
      topicId = topicId,
      title = title
    ).andThen {
      case Success(errors) => if(errors.nonEmpty) {
        // Request went through but Discourse responded with errors
        this.Logger.warn(
          "Request to update project topic was successful but Discourse responded with errors:\n" +
            s"Project: ${project.url}\n" +
            s"Topic ID: $topicId\n" +
            s"Title: $title\n" +
            s"Errors: ${errors.toString}")
      }
      case Failure(e) =>
        // Discourse never received our request!
        this.recovery.failedUpdateAttempts += project.id.get
    }
  }

  def postDiscussionReply(project: Project, user: User, content: String): Future[List[String]] = {
    checkArgument(project.topicId.isDefined, "undefined topic id", "")
    // It's OK if Discourse responds with errors here, we will just show them to the user
    createPost(
      username = user.name,
      topicId = project.topicId.get,
      content = content
    ).map(_.left.toOption.getOrElse(List.empty)).andThen {
      case Failure(e) =>
        List("Could not connect to forums, please try again later.")
    }
  }

  def deleteProjectTopic(project: Project): Future[Boolean] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    checkArgument(project.topicId.isDefined, "undefined topic id", "")
    deleteTopic(project.ownerName, project.topicId.get).andThen {
      case Success(result) => if(!result)
        this.recovery.failedDeleteAttempts += project.id.get
      case Failure(e) =>
        this.recovery.failedDeleteAttempts += project.id.get
    }
  }

  private def buildProjectTitle(project: Project) = project.name + project.description.map(d => s" - $d").getOrElse("")

  private def buildProjectTopic(project: Project)
  = StringUtils.readAndFormatFile(this.topicTemplatePath, project.name, project.url, project.homePage.contents)

}
