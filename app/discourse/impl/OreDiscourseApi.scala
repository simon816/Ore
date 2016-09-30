package discourse.impl

import java.nio.file.Path

import akka.actor.Scheduler
import com.google.common.base.Preconditions.{checkArgument, checkNotNull}
import db.impl.access.ProjectBase
import discourse.{DiscourseApi, DiscourseSSO}
import models.project.{Project, Version}
import models.user.User
import util.StringUtils.readAndFormatFile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

/**
  * An implementation of [[DiscourseApi]] and [[DiscourseSSO]] suited to Ore's
  * needs.
  *
  * Note: It is very important that the implementor of this trait is a
  * singleton, otherwise countless threads will be spawned from this object's
  * [[RecoveryTask]].
  */
trait OreDiscourseApi extends DiscourseApi with DiscourseSSO {

  var projects: ProjectBase = _

  val categorySlug: String
  val topicTemplatePath: Path
  val versionReleasePostTemplatePath: Path
  val retryRate: FiniteDuration
  val scheduler: Scheduler

  val templates: Templates = new Templates

  private var recovery: RecoveryTask = _

  isDebugMode = true

  /**
    * Initializes and starts this API instance.
    */
  def start() = {
    checkNotNull(this.projects, "projects are null", "")
    this.recovery = new RecoveryTask(this.scheduler, this.retryRate, this, this.projects)
    this.recovery.loadUnhealthyData()
    this.recovery.start()
    Logger.info("Discourse API initialized.")
  }

  /**
    * Creates a new topic for the specified [[Project]].
    *
    * @param project Project to create topic for.
    * @return        True if successful
    */
  def createProjectTopic(project: Project): Future[Boolean] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    val content = this.templates.projectTopic(project)
    val title = this.templates.projectTitle(project)
    val resultPromise: Promise[Boolean] = Promise()
    createTopic(
      poster = project.ownerName,
      title = title,
      content = content,
      categorySlug = this.categorySlug
    ).andThen {
      case Success(errorsOrTopic) => errorsOrTopic match {
        case Left(errors) =>
          // Request went through but Discourse responded with errors
          // Don't schedule a retry because this will just keep happening
          Logger.warn(
            "Request to create project topic was successful but Discourse responded with errors:\n" +
              s"Project: ${project.url}\n" +
              s"Title: $title\n" +
              s"Content: $content\n" +
              s"Errors: ${errors.toString}")
          resultPromise.success(false)
        case Right(topic) =>
          // Topic created!
          // Catch some unexpected cases (should never happen)
          if (!topic.isTopic)
            throw new RuntimeException("project post isn't topic?")
          if (topic.userId != project.ownerId)
            throw new RuntimeException("project post user isn't owner?")

          // Update the post and topic id in the project
          project.topicId = topic.topicId
          project.postId = topic.postId

          Logger.info(
            s"New project topic:\n" +
              s"Project: ${project.url}\n" +
              s"Topic ID: ${project.topicId}\n" +
              s"Post ID: ${project.postId}")

          resultPromise.success(true)
      }
      case Failure(e) =>
        // Discourse never received our request! Try again later.
        Logger.info(s"Could not create project topic for project ${project.url}. Rescheduling...")
        this.recovery.failedTopicAttempts += project.id.get
        resultPromise.success(false)
    }

    resultPromise.future
  }

  /**
    * Updates a [[Project]]'s forum topic with the appropriate content.
    *
    * @param project  Project to update topic for
    * @return         True if successful
    */
  def updateProjectTopic(project: Project): Future[Boolean] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    checkArgument(project.topicId != -1, "undefined topic id", "")
    checkArgument(project.postId != -1, "undefined post id", "")

    val topicId = project.topicId
    val postId = project.postId
    val title = this.templates.projectTitle(project)
    val ownerName = project.ownerName

    // Set flag so that if we are interrupted we will remember to do it later
    project.setTopicDirty(true)

    // A promise for our final result
    val resultPromise: Promise[Boolean] = Promise()

    def logErrors(errors: List[String]) = Logger.warn(
      "Request to update project topic was successful but Discourse responded with errors:\n" +
        s"Project: ${project.url}\n" +
        s"Topic ID: $topicId\n" +
        s"Title: $title\n" +
        s"Errors: ${errors.toString}"
    )

    def logFailure() = Logger.info(s"Couldn't update project topic for project ${project.url}. Rescheduling...")

    // Update title
    updateTopic(
      username = ownerName,
      topicId = topicId,
      title = title
    ).andThen {
      case Success(errors) =>
        if (errors.nonEmpty) {
          // Request went through but Discourse responded with errors
          logErrors(errors)
          resultPromise.success(false)
        } else {
          // Title updated! Update the content now
          updatePost(
            username = ownerName,
            postId = postId,
            content = project.homePage.contents
          ).andThen {
            case Success(updateErrors) =>
              if (updateErrors.nonEmpty) {
                logErrors(errors)
                resultPromise.success(false)
              } else {
                // Title and content updated!
                Logger.info(s"Project topic updated for ${project.url}.")

                project.setTopicDirty(false)
                resultPromise.success(true)
              }
            case Failure(e) =>
              logFailure()
              this.recovery.failedUpdateAttempts += project.id.get
              resultPromise.success(false)
          }
        }
      case Failure(e) =>
        // Discourse never received our request!
        logFailure()
        this.recovery.failedUpdateAttempts += project.id.get
        resultPromise.success(false)
    }

    resultPromise.future
  }

  /**
    * Posts a new reply to a [[Project]]'s forum topic.
    *
    * @param project  Project to post to
    * @param user     User who is posting
    * @param content  Post content
    * @return         List of errors Discourse returns
    */
  def postDiscussionReply(project: Project, user: User, content: String): Future[List[String]] = {
    checkArgument(project.topicId != -1, "undefined topic id", "")
    // It's OK if Discourse responds with errors here, we will just show them to the user
    createPost(
      username = user.name,
      topicId = project.topicId,
      content = content
    ).map(_.left.toOption.getOrElse(List.empty)).recover {
      case e: Exception =>
        List("Could not connect to forums, please try again later.")
    }
  }

  /**
    * Posts a new "Version release" to a [[Project]]'s forum topic.
    *
    * @param project Project to post release to
    * @param version Version of project
    * @return
    */
  def postVersionRelease(project: Project, version: Version): Future[List[String]] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    checkArgument(version.id.isDefined, "undefined version", "")
    checkArgument(version.projectId == project.id.get, "invalid version project pair", "")
    // TODO: Handle failure
    // TODO: Need to be able to edit description before release so you can change the post content
    postDiscussionReply(
      project = project,
      user = project.owner,
      content = this.templates.versionRelease(project, version))
  }

  /**
    * Delete's a [[Project]]'s forum topic.
    *
    * @param project  Project to delete topic for
    * @return         True if deleted
    */
  def deleteProjectTopic(project: Project): Future[Boolean] = {
    checkArgument(project.id.isDefined, "undefined project", "")
    checkArgument(project.topicId != -1, "undefined topic id", "")

    def logFailure() = Logger.info(s"Couldn't delete topic for project: ${project.url}. Rescheduling...")

    val resultPromise: Promise[Boolean] = Promise()
    deleteTopic(project.ownerName, project.topicId).andThen {
      case Success(result) =>
        if(!result) {
          logFailure()
          this.recovery.failedDeleteAttempts += project.id.get
          resultPromise.success(false)
        } else {
          project.topicId = -1
          project.postId = -1
          Logger.info(s"Successfully deleted project topic for: ${project.url}.")
          resultPromise.success(true)
        }
      case Failure(e) =>
        logFailure()
        this.recovery.failedDeleteAttempts += project.id.get
        resultPromise.success(false)
    }

    resultPromise.future
  }

  /**
    * Returns a future result of the amount of users on Discourse that are in
    * this list.
    *
    * @param users  Users to check
    * @return       Amount on discourse
    */
  def countUsers(users: List[String]): Future[Int] = {
    var futures: Seq[Future[Boolean]] = Seq.empty
    for (user <- users) {
      futures :+= userExists(user).recover {
        case e: Exception => false
      }
    }
    Future.sequence(futures).map(results => results.count(_ == true))
  }

  /**
    * Discourse content templates.
    */
  class Templates {

    /** Creates a new title for a project topic. */
    def projectTitle(project: Project) = project.name + project.description.map(d => s" - $d").getOrElse("")

    /** Generates the content for a project topic. */
    def projectTopic(project: Project) = readAndFormatFile(
      OreDiscourseApi.this.topicTemplatePath,
      project.name,
      project.url,
      project.homePage.contents
    )

    /** Generates the content for a version release post. */
    def versionRelease(project: Project, version: Version) = readAndFormatFile(
      OreDiscourseApi.this.versionReleasePostTemplatePath,
      project.name,
      project.url,
      OreDiscourseApi.this.url,
      version.description.getOrElse("*No description given.*")
    )

  }

}
