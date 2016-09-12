package forums

import models.project.Project
import models.user.User
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Handles forum topic management for Projects.
  *
  * @param url        Forum URL
  * @param key        Discourse API key
  * @param categoryId Discourse category ID
  * @param ws         HTTP client
  */
class DiscourseEmbeddingService(api: DiscourseApi,
                                url: String,
                                key: String,
                                categoryId: Int,
                                ws: WSClient,
                                config: OreConfig,
                                implicit val env: OreEnv) {

  import api.{FatalForumErrorException, validate, sync}
  import config.debug

  /**
    * Creates a new topic for the specified [[Project]].
    *
    * @param project Project to create topic for
    */
  def createTopic(project: Project): Unit = {
    debug("Creating topic for " + project.name, 3)

    if (!project.isDefined)
      throw UndefinedProjectException

    // Prepare request
    val username = project.ownerName
    val params = this.keyedRequest(username) + (
      "title" -> Seq(project.name + project.description.map(" - " + _).getOrElse("")),
      "raw" -> Seq(project.topicContent))

    // Send request
    this.ws.url(url + "/posts").post(params).map { response =>
      validate(response) match {
        case Left(errors) =>
          throw FatalForumErrorException(errors)
        case Right(json) =>
          val postId = (json \ "id").as[Int]
          val topicId = (json \ "topic_id").as[Int]
          project.topicId = topicId
          project.postId = postId

          updateTopicCategory(username, topicId, url)
      }
    } recover {
      case e: Exception =>
        debug("Failed to create topic, rescheduling: " + e.getMessage, 3)
        sync.scheduleRetry(() => createTopic(project))
        throw e
    }
  }

  private def updateTopicCategory(username: String, topicId: Int, url: String): Unit = {
    val update = this.keyedRequest(username) + (
      "topic_id" -> Seq(topicId.toString),
      "category_id" -> Seq(this.categoryId.toString))

    this.ws.url(url + "/t/" + topicId).put(update).andThen {
      case result =>
        debug("CATEGORY SET: " + result.get, 3)
        debug(result.get.json, 3)
    } recover {
      case e: Exception =>
        debug("Failed to update topic category, rescheduling: " + e.getMessage, 3)
        sync.scheduleRetry(() => updateTopicCategory(username, topicId, url))
        throw e
    }
  }

  /**
    * Updates the specified [[Project]]'s topic with the appropriate content.
    *
    * @param project Project to update
    */
  def updateTopic(project: Project): Unit = {
    if (!project.isDefined)
      throw UndefinedProjectException

    project.postId match {
      case None =>
        createTopic(project)
      case Some(postId) =>
        val params = this.keyedRequest(project.ownerName) + ("post[raw]" -> Seq(project.topicContent))
        ws.url(url + "/posts/" + postId).put(params).andThen {
          case r =>
            debug("TOPIC UPDATE: " + r.get, 3)
            debug(r.get.json, 3)
        } recover {
          case e: Exception =>
            debug("Failed to update topic, rescheduling: " + e.getMessage, 3)
            sync.scheduleRetry(() => updateTopic(project))
            throw e
        }
    }
  }

  /**
    * Performs a rename on the forum topic for the specified [[Project]].
    *
    * @param project Project that was renamed
    */
  def renameTopic(project: Project): Unit = {
    if (!project.isDefined)
      throw UndefinedProjectException

    project.topicId match {
      case None =>
        createTopic(project)
      case Some(topicId) =>
        val params = this.keyedRequest(project.ownerName) + (
          "topic_id" -> Seq(topicId.toString),
          "title" -> Seq(project.name + project.description.map(" - " + _).getOrElse("")))

        this.ws.url(url + "/t/" + topicId).put(params).andThen {
          case r =>
            debug("TOPIC RENAME: " + r.get, 3)
            debug(r.get.json, 3)
        } recover {
          case e: Exception =>
            debug("Failed to rename topic, rescheduling: " + e.getMessage)
            sync.scheduleRetry(() => renameTopic(project))
            throw e
        }
    }
  }

  /**
    * Delete's the topic for the specified [[Project]].
    *
    * @param project Project to delete topic for
    */
  def deleteTopic(project: Project): Unit = {
    if (!project.isDefined)
      throw UndefinedProjectException

    val k = "api_key" -> this.key
    val u = "api_username" -> project.ownerName

    project.topicId.foreach { topicId =>
      this.ws.url(url + "/t/" + project.topicId.get).withQueryString(k, u).delete().andThen {
        case r =>
          debug("TOPIC DELETE: " + r.get, 3)
      } recover {
        case e: Exception =>
          debug("Failed to delete topic, rescheduling: " + e.getMessage, 3)
          sync.scheduleRetry(() => deleteTopic(project))
          throw e
      }
    }
  }

  /**
    * Posts a new reply to the specified [[Project]] topic as the specified
    * [[User]].
    *
    * @param project  Project to post to
    * @param user     User to post as
    * @param content  Content to post
    */
  def postReply(project: Project, user: User, content: String): Future[Option[List[String]]] = {
    if (!project.isDefined)
      throw UndefinedProjectException
    if (!user.isDefined)
      throw new RuntimeException("undefined user")

    project.topicId match {
      case None =>
        throw new RuntimeException("user tried to post reply to project without a topic")
      case Some(topicId) =>
        val params = this.keyedRequest(user.username) + (
          "topic_id" -> Seq(topicId.toString),
          "raw" -> Seq(content))
        this.ws.url(url + "/posts").post(params).map(validate(_).left.toOption)
    }
  }

  private def keyedRequest(username: String) = Map("api_key" -> Seq(this.key), "api_username" -> Seq(username))

  private def UndefinedProjectException = new RuntimeException("undefined project")

}

object DiscourseEmbeddingService {

  /**
    * Represents a disabled state of [[DiscourseEmbeddingService]].
    */
  object Disabled extends DiscourseEmbeddingService(null, null, null, -1, null, null, null) {
    override def createTopic(project: Project) = Future(None)
    override def updateTopic(project: Project) = Future(null)
    override def renameTopic(project: Project) = Future(null)
    override def deleteTopic(project: Project) = Future(null)
    override def postReply(project: Project, user: User, content: String) = Future(null)
  }

}
