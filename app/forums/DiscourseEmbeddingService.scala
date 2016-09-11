package forums

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import models.project.Project
import models.user.User
import play.api.libs.json.JsArray
import play.api.libs.ws.WSClient
import util.{OreConfig, OreEnv}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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
                                actorSystem: ActorSystem,
                                implicit val env: OreEnv) {

  import api.validate
  import config.debug

  val sync = new DiscourseSync(
    actorSystem.scheduler, Duration(this.config.forums.getInt("embed.retryRate").get, TimeUnit.MILLISECONDS))

  /**
    * Creates a new topic for the specified [[Project]].
    *
    * @param project Project to create topic for
    */
  def createTopic(project: Project): Unit = {
    debug("Creating topic for " + project.name, 3)

    if (!project.isDefined)
      return

    // Prepare request
    val username = project.ownerName
    val params = this.keyedRequest(username) + (
      "title" -> Seq(project.name + project.description.map(" - " + _).getOrElse("")),
      "raw" -> Seq(project.topicContent))

    // Send request
    this.ws.url(url + "/posts").post(params).map { response =>
      validate(response) { json =>
        debug("TOPIC CREATED: " + json, 3)

        val postId = (json \ "id").as[Int]
        val topicId = (json \ "topic_id").as[Int]
        project.topicId = topicId
        project.postId = postId

        updateTopicCategory(username, topicId, url)
      }
    } recover {
      case e: Exception =>
        debug("Failed to create topic, rescheduling: " + e.getMessage, 3)
        this.sync.scheduleRetry(() => createTopic(project))
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
        this.sync.scheduleRetry(() => updateTopicCategory(username, topicId, url))
    }
  }

  /**
    * Updates the specified [[Project]]'s topic with the appropriate content.
    *
    * @param project Project to update
    */
  def updateTopic(project: Project): Unit = {
    if (!project.isDefined)
      return

    val postId = project.postId.get
    val params = this.keyedRequest(project.ownerName) + ("post[raw]" -> Seq(project.topicContent))
    ws.url(url + "/posts/" + postId).put(params).andThen {
      case r =>
        debug("TOPIC UPDATE: " + r.get, 3)
        debug(r.get.json, 3)
    } recover {
      case e: Exception =>
        debug("Failed to update topic, rescheduling: " + e.getMessage, 3)
        this.sync.scheduleRetry(() => updateTopic(project))
    }
  }

  /**
    * Performs a rename on the forum topic for the specified [[Project]].
    *
    * @param project Project that was renamed
    */
  def renameTopic(project: Project): Unit = {
    if (!project.isDefined)
      return

    val topicId = project.topicId.get
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
        this.sync.scheduleRetry(() => renameTopic(project))
    }
  }

  /**
    * Delete's the topic for the specified [[Project]].
    *
    * @param project Project to delete topic for
    */
  def deleteTopic(project: Project): Unit = {
    if (!project.isDefined)
      return

    val k = "api_key" -> this.key
    val u = "api_username" -> project.ownerName

    this.ws.url(url + "/t/" + project.topicId.get).withQueryString(k, u).delete().andThen {
      case r =>
        debug("TOPIC DELETE: " + r.get, 3)
    } recover {
      case e: Exception =>
        debug("Failed to delete topic, rescheduling: " + e.getMessage, 3)
        this.sync.scheduleRetry(() => deleteTopic(project))
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
  def postReply(project: Project, user: User, content: String): Future[Option[String]] = {
    if (!project.isDefined || !user.isDefined)
      return Future(None)

    val params = this.keyedRequest(user.username) + (
      "topic_id" -> Seq(project.topicId.get.toString),
      "raw" -> Seq(content))

    this.ws.url(url + "/posts").post(params).map { r =>
      val json = r.json
      debug("NEW POST: " + r, 3)
      debug(json, 3)
      (json \ "errors").asOpt[JsArray].flatMap(_(0).asOpt[String])
    }
  }

  private def keyedRequest(username: String) = {
    Map("api_key" -> Seq(this.key), "api_username" -> Seq(username))
  }

}

object DiscourseEmbeddingService {

  /**
    * Represents a disabled state of [[DiscourseEmbeddingService]].
    */
  object Disabled extends DiscourseEmbeddingService(null, null, null, -1, null, null, null, null) {
    override def createTopic(project: Project) = Future(None)
    override def updateTopic(project: Project) = Future(null)
    override def renameTopic(project: Project) = Future(null)
    override def deleteTopic(project: Project) = Future(null)
    override def postReply(project: Project, user: User, content: String) = Future(null)
  }

}
